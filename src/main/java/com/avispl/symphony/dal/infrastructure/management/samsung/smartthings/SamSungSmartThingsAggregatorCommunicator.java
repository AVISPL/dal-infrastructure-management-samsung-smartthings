package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings;

import java.math.RoundingMode;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.math.IntMath;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.AggregatorGroupControllingMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.DeviceCategoriesMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.DeviceDisplayTypesMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.HubInfoMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsConstant;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsURL;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.location.LocationManagementMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.room.CreateRoomMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.room.RoomManagementMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.Hub;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.Device;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceHealth;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location.Location;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location.LocationWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DashboardPresentation;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DetailViewPresentation;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DevicePresentation;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.Language;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.PoCode;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.room.Room;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.room.RoomWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.sence.Scene;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.sence.SceneWrapper;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * SamSungSmartThingsAggregatorCommunicator
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/22/2022
 * @since 1.0.0
 */
public class SamSungSmartThingsAggregatorCommunicator extends RestCommunicator implements Aggregator, Monitorable, Controller {

	/**
	 * Process that is running constantly and triggers collecting data from Zoom API endpoints, based on the given timeouts and thresholds.
	 *
	 * @author Maksym.Rossiytsev
	 * @since 1.0.0
	 */
	class SamSungSmartThingsDeviceDataLoader implements Runnable {
		private volatile boolean inProgress;
		private volatile int threadIndex;
		private List<String> deviceIds;

		/**
		 * Parameters constructors
		 *
		 * @param threadIndex index of thread
		 * @param deviceIds list of device IDs
		 */
		public SamSungSmartThingsDeviceDataLoader(int threadIndex, List<String> deviceIds) {
			inProgress = true;
			this.threadIndex = threadIndex;
			this.deviceIds = deviceIds;
		}

		@Override
		public void run() {
			mainloop:
			while (inProgress) {
				if (!inProgress) {
					break mainloop;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Fetching other than SmartThings device list" + threadIndex);
				}
				Long currentTime = System.currentTimeMillis();
				if (!cachedDevices.isEmpty()) {
					retrieveDeviceDetail(threadIndex);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("finished for" + threadIndex + "   " + (System.currentTimeMillis() - currentTime));
				}
				if (!inProgress) {
					break mainloop;
				}
				int aggregatedDevicesCount = aggregatedDevices.size();
				if (aggregatedDevicesCount == 0) {
					continue mainloop;
				}
				if (!aggregatedDevices.isEmpty()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Aggregated devices after applying filter: " + aggregatedDevices);
					}
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Finished collecting devices statistics cycle at " + new Date());
				}
			}
			// Finished collecting
		}

		//region retrieve detail aggregated device info: device health, device presentation and device full status in worker thread
		//--------------------------------------------------------------------------------------------------------------------------------

		/**
		 * @param  currentThread current thread index
		 *
		 * Submit thread to get device detail info
		 */
		private void retrieveDeviceDetail(int currentThread) {
			Set<String> presentationIds = cachedDevices.values().stream().map(Device::getPresentationId).collect(Collectors.toSet());
			int currentPhaseIndex = currentPhase.get() - SmartThingsConstant.CONVERT_POSITION_TO_INDEX;
			int devicesPerPollingIntervalQuantity = IntMath.divide(cachedDevices.size(), localPollingInterval, RoundingMode.CEILING);

			List<String> deviceIdsInThread;
			if (currentThread == deviceStatisticsCollectionThreads - SmartThingsConstant.CONVERT_POSITION_TO_INDEX) {
				// add the rest of the devices for a monitoring interval to the last thread
				deviceIdsInThread = this.deviceIds.stream()
						.skip(currentPhaseIndex * devicesPerPollingIntervalQuantity + currentThread * SmartThingsConstant.MAX_DEVICE_QUANTITY_PER_THREAD)
						.limit(devicesPerPollingIntervalQuantity - SmartThingsConstant.MAX_DEVICE_QUANTITY_PER_THREAD * currentThread)
						.collect(Collectors.toList());
			} else {
				deviceIdsInThread = this.deviceIds.stream()
						.skip(currentPhaseIndex * devicesPerPollingIntervalQuantity + currentThread * SmartThingsConstant.MAX_DEVICE_QUANTITY_PER_THREAD)
						.limit(SmartThingsConstant.MAX_DEVICE_QUANTITY_PER_THREAD)
						.collect(Collectors.toList());
			}
			try {
				for (String deviceId : deviceIdsInThread) {
					Long startTime = System.currentTimeMillis();
					retrieveDeviceHealth(deviceId);

					String devicePresentationId = cachedDevices.get(deviceId).getPresentationId();
					if (presentationIds.contains(devicePresentationId)) {
						retrieveDevicePresentation(deviceId);
						presentationIds.remove(devicePresentationId);
					}

					retrieveDeviceFullStatus(deviceId);
					mapDevicesToAggregatedDevice(cachedDevices.get(deviceId));
					if (logger.isDebugEnabled()) {
						Long time = System.currentTimeMillis() - startTime;
						logger.debug(String.format("Finished fetch %s details info in worker thread: %s", cachedDevices.get(deviceId).getName(), time));
					}
				}
			} catch (Exception e) {
				retrieveDeviceDetail(currentThread);
				logger.error(String.format("Exception during retrieve '%s' data processing", e.getMessage()), e);
			}
		}

		/**
		 * This method is used to retrieve list of device health by send GET request to https://api.smartthings.com/v1/devices/{deviceId}/health
		 *
		 * @param deviceID id of device
		 * @throws ResourceNotReachableException If any error occurs
		 */
		private void retrieveDeviceHealth(String deviceID) {
			Objects.requireNonNull(deviceID);
			try {
				String request = SmartThingsURL.DEVICES
						.concat(SmartThingsConstant.SLASH)
						.concat(deviceID)
						.concat(SmartThingsURL.DEVICE_HEALTH);

				DeviceHealth deviceHealth = doGet(request, DeviceHealth.class);

				if (deviceHealth != null) {
					cachedDevices.get(deviceID).setState(deviceHealth.getState());
				} else {
					throw new ResourceNotReachableException(String.format("%s health info is empty", cachedDevices.get(deviceID).getName()));
				}
			} catch (Exception e) {
				failedMonitoringDeviceIds.add(deviceID);
				if (logger.isErrorEnabled()) {
					logger.error(String.format("Error while retrieve %s health info: %s ", cachedDevices.get(deviceID).getName(), e.getMessage()), e);
				}
			}
		}

		/**
		 * This method is used to retrieve list of device health by send GET request to
		 * https://api.smartthings.com/v1/presentation?presentationId={presentationId}&manufacturerName={manufacturerName}
		 *
		 * @param deviceId device ID
		 * @throws ResourceNotReachableException If any error occurs
		 */
		private void retrieveDevicePresentation(String deviceId) {
			Device device = cachedDevices.get(deviceId);
			try {
				String request = SmartThingsURL.PRESENTATION
						.concat(SmartThingsConstant.QUESTION_MARK)
						.concat(SmartThingsURL.PRESENTATION_ID)
						.concat(device.getPresentationId())
						.concat(SmartThingsConstant.AMPERSAND)
						.concat(SmartThingsURL.MANUFACTURE_NAME)
						.concat(device.getManufacturerName());

				ObjectNode objectNode = doGet(request, ObjectNode.class);
				DevicePresentation devicePresentation = objectMapper.readValue(objectNode.toString(), DevicePresentation.class);

				if (devicePresentation != null) {
					for (Device dv : cachedDevices.values()) {
						if (device.getPresentationId().equals(dv.getPresentationId())) {
							cachedDevices.get(dv.getDeviceId()).setPresentation(devicePresentation);
						}
					}
				} else {
					throw new ResourceNotReachableException(String.format("%s presentation info is empty", device.getName()));
				}
			} catch (Exception e) {
				failedMonitoringDeviceIds.add(deviceId);
				if (logger.isErrorEnabled()) {
					logger.error(String.format("Error while retrieve %s presentation info: %s", device.getName(), e.getMessage()), e);
				}
			}
		}

		/**
		 * This method is used to retrieve device full status by send GET request to
		 * https://api.smartthings.com/v1/devices/{deviceId}/status
		 *
		 * @param deviceId device ID
		 * @throws ResourceNotReachableException If any error occurs
		 */
		private void retrieveDeviceFullStatus(String deviceId) {
			try {
				String request = SmartThingsURL.DEVICES
						.concat(SmartThingsConstant.SLASH)
						.concat(deviceId)
						.concat(SmartThingsURL.STATUS);

				ObjectNode response = doGet(request, ObjectNode.class);

				Optional<List<DetailViewPresentation>> detailViewPresentations = Optional.ofNullable(cachedDevices.get(deviceId))
						.map(Device::getPresentation)
						.map(DevicePresentation::getDetailViewPresentations);

				Optional<Language> language = Optional.ofNullable(cachedDevices.get(deviceId))
						.map(Device::getPresentation)
						.map(DevicePresentation::getLanguages)
						.map(l -> l.stream().filter(lg -> SmartThingsConstant.ENG_LOCALE.equals(lg.getLocale())).findFirst().get());

				Optional<List<DetailViewPresentation>> dashBoardActions = Optional.ofNullable(cachedDevices.get(deviceId))
						.map(Device::getPresentation)
						.map(DevicePresentation::getDashboardPresentations)
						.map(DashboardPresentation::getActions);

				if (response != null && detailViewPresentations.isPresent() && language.isPresent()) {
					mapControllablePropertiesLabelByLocale(language.get(), detailViewPresentations.get());

					List<DetailViewPresentation> detailViewPresentationsAfterMapping = mapControllablePropertyStatusToDevice(response, detailViewPresentations.get());
					cachedDevices.get(deviceId).getPresentation().setDetailViewPresentations(detailViewPresentationsAfterMapping);

					if (dashBoardActions.isPresent()) {
						List<DetailViewPresentation> dashboardActionsAfterMapping = mapControllablePropertyStatusToDevice(response, dashBoardActions.get());
						cachedDevices.get(deviceId).getPresentation().getDashboardPresentations().setActions(dashboardActionsAfterMapping);
					}
				} else {
					throw new ResourceNotReachableException(String.format("%s presentation info is empty", cachedDevices.get(deviceId).getName()));
				}
			} catch (Exception e) {
				failedMonitoringDeviceIds.add(deviceId);
				if (logger.isErrorEnabled()) {
					logger.error(String.format("Error while retrieve %s presentation info: %s", cachedDevices.get(deviceId).getName(), e.getMessage()), e);
				}
			}
		}

		/**
		 * This method is used to map controllable property status to device
		 *
		 * @param response Json response
		 * @param detailViewPresentations list of detail view presentation of device
		 */
		private List<DetailViewPresentation> mapControllablePropertyStatusToDevice(ObjectNode response, List<DetailViewPresentation> detailViewPresentations) {

			for (DetailViewPresentation detailViewPresentation : detailViewPresentations) {
				Optional<Iterator<JsonNode>> capabilitiesStatus = Optional.ofNullable(response.elements().next())
						.map(JsonNode::elements)
						.map(Iterator::next)
						.map(c -> c.get(detailViewPresentation.getCapability()))
						.map(JsonNode::elements);

				String value = SmartThingsConstant.NONE;
				String unit = SmartThingsConstant.NONE;

				if (capabilitiesStatus.isPresent() && capabilitiesStatus.get().hasNext()) {
					value = Optional.ofNullable(capabilitiesStatus.get().next())
							.map(u -> u.get(SmartThingsConstant.VALUE))
							.map(JsonNode::asText)
							.orElse(SmartThingsConstant.NONE);
				}

				switch (detailViewPresentation.getDisplayType()) {
					case DeviceDisplayTypesMetric.SLIDER:
						if (capabilitiesStatus.isPresent() && capabilitiesStatus.get().hasNext()) {
							unit = Optional.ofNullable(capabilitiesStatus.get().next())
									.map(u -> u.get(SmartThingsConstant.UNIT))
									.map(JsonNode::asText)
									.orElse(SmartThingsConstant.NONE);
						}
						detailViewPresentation.getSlider().setValue(value);
						detailViewPresentation.getSlider().setUnit(unit);
						break;
					case DeviceDisplayTypesMetric.STAND_BY_POWER_SWITCH:
					case DeviceDisplayTypesMetric.TOGGLE_SWITCH:
					case DeviceDisplayTypesMetric.SWITCH:
						detailViewPresentation.getStandbyPowerSwitch().setValue(value);
						break;
					case DeviceDisplayTypesMetric.PUSH_BUTTON:
						detailViewPresentation.getPushButton().setValue(value);
						break;
					case DeviceDisplayTypesMetric.LIST:
						detailViewPresentation.getDropdownList().setValue(value);
						break;
					case DeviceDisplayTypesMetric.NUMBER_FIELD:
						detailViewPresentation.getNumberField().setValue(value);
						break;
					case DeviceDisplayTypesMetric.TEXT_FIELD:
						detailViewPresentation.getTextField().setValue(value);
						break;
					case DeviceDisplayTypesMetric.STATE:
						break;
					default:
						if (logger.isWarnEnabled()) {
							logger.warn(String.format("Unexpected device display type: %s", detailViewPresentation.getDisplayType()));
						}
						break;
				}
			}
			return detailViewPresentations;
		}

		/**
		 * This method is used to map controllable properties by locale
		 *
		 * @param language label by language
		 * @param detailViewPresentations list of detail view presentation of device
		 */
		private void mapControllablePropertiesLabelByLocale(Language language, List<DetailViewPresentation> detailViewPresentations) {
			Optional<List<PoCode>> poCodes = Optional.ofNullable(language.getPoCodes());
			if (poCodes.isPresent()) {
				for (DetailViewPresentation detailViewPresentation : detailViewPresentations) {
					for (PoCode poCode : poCodes.get()) {
						if (detailViewPresentation.getLabel().equals(poCode.getPo())) {
							detailViewPresentation.setLabel(poCode.getLabel());
							break;
						}
					}
				}
			}
		}

		/**
		 * This method is used to map device info in DTO to Aggregated device
		 *
		 * @param device device info
		 */
		private void mapDevicesToAggregatedDevice(Device device) {
			Objects.requireNonNull(device);
			AggregatedDevice aggregatedDevice = new AggregatedDevice();
			aggregatedDevice.setDeviceId(device.getDeviceId());
			aggregatedDevice.setCategory(getDefaultValueForNullData(device.retrieveCategory(), SmartThingsConstant.NONE));
			aggregatedDevice.setDeviceName(getDefaultValueForNullData(device.getName(), SmartThingsConstant.NONE));
			aggregatedDevice.setDeviceOnline(convertDeviceStatusValue(device));
			aggregatedDevices.put(device.getDeviceId(), aggregatedDevice);
		}

		/**
		 * convert device Online/ Offline status to boolean
		 *
		 * @param device device info
		 * @return boolean
		 */
		private boolean convertDeviceStatusValue(Device device) {
			Objects.requireNonNull(device);
			if (StringUtils.isNotNullOrEmpty(device.getState())) {
				return SmartThingsConstant.ONLINE.equals(device.getState());
			}
			return false;
		}

		//--------------------------------------------------------------------------------------------------------------------------------
		//endregion

		/**
		 * Triggers main loop to stop
		 */
		public void stop() {
			inProgress = false;
		}
	}

	/**
	 * Number of threads in a thread pool reserved for the device statistics collection
	 */
	private volatile int deviceStatisticsCollectionThreads;

	/**
	 * SmartThings personal access token
	 */
	private String apiToken;

	/**
	 * Caching the list of locations
	 */
	private List<Location> cachedLocations = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Caching the list rooms data
	 */
	private List<Room> cachedRooms = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Caching create room data
	 */
	private Room cachedCreateRoom = new Room();

	/**
	 * Caching the list of scenes data
	 */
	private List<Scene> cachedScenes = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Caching the list of devices data
	 */
	private ConcurrentHashMap<String, Device> cachedDevices = new ConcurrentHashMap<>();

	/**
	 * Caching the list of devices data after polling interval
	 */
	private ConcurrentHashMap<String, Device> cachedDevicesAfterPollingInterval = new ConcurrentHashMap<>();
	/**
	 * Caching the list of device Ids
	 */
	private List<String> deviceIds = new ArrayList<>();

	/**
	 * Caching the list of failed monitoring devices
	 */
	private Set<String> failedMonitoringDeviceIds = ConcurrentHashMap.newKeySet();

	/**
	 * Runner service responsible for collecting data
	 */
	private SamSungSmartThingsDeviceDataLoader deviceDataLoader;

	/**
	 * List of aggregated devices
	 */
	private ConcurrentHashMap<String, AggregatedDevice> aggregatedDevices = new ConcurrentHashMap<>();

	/**
	 * Executor that runs all the async operations, that {@link #deviceDataLoader} is posting and
	 * {@link #devicesExecutionPool} is keeping track of
	 */
	private static ExecutorService executorService;

	/**
	 * Pool for keeping all the async operations in, to track any operations in progress and cancel them if needed
	 */
	private List<Future> devicesExecutionPool = new ArrayList<>();

	/**
	 * ReentrantLock to prevent null pointer exception to localExtendedStatistics when controlProperty method is called before GetMultipleStatistics method.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	// Adapter properties
	private String locationFilter;
	private String poolingInterval;

	private String locationIdFiltered;
	private ExtendedStatistics localExtendedStatistics;
	private boolean isEmergencyDelivery = false;
	private Boolean isEditedForCreateRoom = false;
	private ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Pooling interval which applied in adapter
	 */
	private volatile int localPollingInterval = SmartThingsConstant.MIN_POOLING_INTERVAL;

	/**
	 * The current phase of monitoring cycle in pooling interval
	 */
	private final AtomicInteger currentPhase = new AtomicInteger(0);

	/**
	 * Retrieves {@code {@link #locationFilter}}
	 *
	 * @return value of {@link #locationFilter}
	 */
	public String getLocationFilter() {
		return locationFilter;
	}

	/**
	 * Sets {@code locationFilter}
	 *
	 * @param locationFilter the {@code java.lang.String} field
	 */
	public void setLocationFilter(String locationFilter) {
		this.locationFilter = locationFilter;
	}

	/**
	 * Retrieves {@code {@link #poolingInterval}}
	 *
	 * @return value of {@link #poolingInterval}
	 */
	public String getPoolingInterval() {
		return poolingInterval;
	}

	/**
	 * Sets {@code poolingInterval}
	 *
	 * @param poolingInterval the {@code java.lang.String} field
	 */
	public void setPoolingInterval(String poolingInterval) {
		this.poolingInterval = poolingInterval;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called.");
		}
		apiToken = this.getPassword();
		this.setBaseUri(SmartThingsURL.BASE_URI);
		super.internalInit();
	}

	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Getting statistics from the device X4 decoder at host %s with port %s", this.host, this.getPort()));
		}
		reentrantLock.lock();
		try {
			if (!validateApiToken()) {
				throw new ResourceNotReachableException("Personal access token cannot be null or empty, please enter valid token in the password field.");
			}
			ExtendedStatistics extendedStatistics = new ExtendedStatistics();
			Map<String, String> stats = new HashMap<>();
			List<AdvancedControllableProperty> advancedControllableProperties = new ArrayList<>();

			if (currentPhase.get() == localPollingInterval) {
				cachedDevicesAfterPollingInterval = (ConcurrentHashMap<String, Device>) cachedDevices.values().stream().collect(Collectors.toConcurrentMap(Device::getDeviceId, Device::new));
			}

			retrieveInfo(stats, advancedControllableProperties);
			if (!cachedDevices.isEmpty() && !isEmergencyDelivery) {
				populateCreateRoomManagement(stats, advancedControllableProperties);
				retrieveScenes(stats, advancedControllableProperties, true);

				String hubId = findDeviceIdByCategory(DeviceCategoriesMetric.HUB.getName());
				if (!hubId.isEmpty()) {
					retrieveHubDetailInfo(stats, hubId, true);
					retrieveHubHealth(stats, hubId, true);
				}

				populateDeviceView(stats, advancedControllableProperties);

				extendedStatistics.setStatistics(stats);
				extendedStatistics.setControllableProperties(advancedControllableProperties);
				localExtendedStatistics = extendedStatistics;
			}
			isEmergencyDelivery = false;

			if (currentPhase.get() == SmartThingsConstant.FIRST_MONITORING_CYCLE_OF_POLLING_INTERVAL) {
				localPollingInterval = calculatingLocalPoolingInterval();
				deviceStatisticsCollectionThreads = calculatingThreadQuantity();
				deviceIds = pushFailedMonitoringDevicesIDToPriority();
			}

			if (currentPhase.get() == localPollingInterval) {
				currentPhase.set(0);
			}
			currentPhase.incrementAndGet();

			if (executorService == null) {
				executorService = Executors.newFixedThreadPool(deviceStatisticsCollectionThreads);
			}
			for (int threadNumber = 0; threadNumber < deviceStatisticsCollectionThreads; threadNumber++) {
				executorService.submit(deviceDataLoader = new SamSungSmartThingsDeviceDataLoader(threadNumber, deviceIds));
			}
		} finally {
			reentrantLock.unlock();
		}
		return Collections.singletonList(localExtendedStatistics);
	}

	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		String property = controllableProperty.getProperty();
		String value = String.valueOf(controllableProperty.getValue());
		reentrantLock.lock();
		try {
			if (this.localExtendedStatistics == null) {
				return;
			}
			Map<String, String> stats = this.localExtendedStatistics.getStatistics();
			List<AdvancedControllableProperty> advancedControllableProperties = this.localExtendedStatistics.getControllableProperties();

			if (this.logger.isDebugEnabled()) {
				this.logger.debug("controlProperty property " + property);
				this.logger.debug("controlProperty value " + value);
			}
			String[] splitProperty = property.split(SmartThingsConstant.HASH);
			if (splitProperty.length != 2) {
				throw new IllegalArgumentException("Unexpected length of control property");
			}
			AggregatorGroupControllingMetric managementGroupMetric = AggregatorGroupControllingMetric.getByName(splitProperty[0].concat(SmartThingsConstant.HASH));

			switch (managementGroupMetric) {
				case LOCATION_MANAGEMENT:
					locationControl(stats, advancedControllableProperties, splitProperty[1], value);
					break;
				case ROOM_MANAGEMENT:
					roomControl(stats, advancedControllableProperties, splitProperty[1], value);
					break;
				case SCENE:
					sceneControl(stats, advancedControllableProperties, splitProperty[1]);
					break;
				case CREATE_ROOM:
					createRoomControl(stats, advancedControllableProperties, splitProperty[1], value);
					break;
				case DEVICES_DASHBOARD:
					deviceDashboardControl(stats, advancedControllableProperties, splitProperty[1], value);
					break;
				default:
					if (logger.isWarnEnabled()) {
						logger.warn(String.format("Controlling group %s is not supported.", managementGroupMetric.getName()));
					}
					throw new IllegalStateException(String.format("Controlling group %s is not supported.", managementGroupMetric.getName()));
			}
		} finally {
			reentrantLock.unlock();
		}
	}

	@Override
	public void controlProperties(List<ControllableProperty> controllablePropertyList) throws Exception {
		if (CollectionUtils.isEmpty(controllablePropertyList)) {
			throw new IllegalArgumentException("Controllable properties cannot be null or empty");
		}
		for (ControllableProperty controllableProperty : controllablePropertyList) {
			controlProperty(controllableProperty);
		}
	}

	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
		if (logger.isWarnEnabled()) {
			logger.warn("Start call retrieveMultipleStatistic");
		}
		if (validateApiToken() && executorService == null) {
			// Due to the bug that after changing properties on fly - the adapter is destroyed but adapter is not initialized properly,
			// so executor service is not running. We need to make sure executorService exists
			executorService = Executors.newFixedThreadPool(8);
			for (int i = 0; i < deviceStatisticsCollectionThreads; i++) {
				executorService.submit(deviceDataLoader = new SamSungSmartThingsDeviceDataLoader(i, deviceIds));
			}
		}
		return aggregatedDevices.values().stream().collect(Collectors.toList());
	}

	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics(List<String> listDeviceId) throws Exception {
		return retrieveMultipleStatistics().stream().filter(aggregatedDevice -> listDeviceId.contains(aggregatedDevice.getDeviceId())).collect(Collectors.toList());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal destroy is called.");
		}

		if (currentPhase.get() == localPollingInterval) {
			if (deviceDataLoader != null) {
				deviceDataLoader.stop();
				deviceDataLoader = null;
			}

			if (executorService != null) {
				executorService.shutdownNow();
				executorService = null;
			}

			devicesExecutionPool.forEach(future -> future.cancel(true));
			devicesExecutionPool.clear();

			aggregatedDevices.clear();
			cachedDevices.clear();
			super.internalDestroy();
		}
	}

	@Override
	protected void authenticate() throws Exception {
		// The aggregator have its own authorization method
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) {
		headers.setBearerAuth(apiToken);
		return headers;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * SmartThings api endpoint does not have ICMP available, so this workaround is needed to provide
	 * ping latency information to Symphony
	 */
	@Override
	public int ping() throws Exception {
		if (isInitialized()) {
			long pingResultTotal = 0L;

			for (int i = 0; i < this.getPingAttempts(); i++) {
				long startTime = System.currentTimeMillis();

				String[] hostSplit = this.getHost().split(SmartThingsConstant.SLASH);
				String host = hostSplit[0];
				try (Socket puSocketConnection = new Socket(host, this.getPort())) {
					puSocketConnection.setSoTimeout(this.getPingTimeout());
					if (puSocketConnection.isConnected()) {
						long pingResult = System.currentTimeMillis() - startTime;
						pingResultTotal += pingResult;
						if (this.logger.isTraceEnabled()) {
							this.logger.trace(String.format("PING OK: Attempt #%s to connect to %s on port %s succeeded in %s ms", i + 1, host, this.getPort(), pingResult));
						}
					} else {
						if (this.logger.isDebugEnabled()) {
							this.logger.debug(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
						}
						return this.getPingTimeout();
					}
				} catch (SocketTimeoutException | ConnectException tex) {
					if (this.logger.isDebugEnabled()) {
						this.logger.error(String.format("PING TIMEOUT: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
					}
					return this.getPingTimeout();
				} catch (Exception e) {
					if (this.logger.isDebugEnabled()) {
						this.logger.error(String.format("PING TIMEOUT: Connection to %s did not succeed, UNKNOWN ERROR %s: ", host, e.getMessage()));
					}
					return this.getPingTimeout();
				}
			}
			return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
		} else {
			throw new IllegalStateException("Cannot use device class without calling init() first");
		}
	}

	//region retrieve aggregator info: hub inventory, hub heath, rooms, scene
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used to retrieve hub detail info by send GET request to "https://api.smartthings.com/v1/hubdevices/{hubId}"
	 *
	 * @param stats store all statistics
	 * @param hubId id of SmartThings hub
	 * @param retryOnError retry if any error occurs
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveHubDetailInfo(Map<String, String> stats, String hubId, boolean retryOnError) {
		try {
			String request = SmartThingsURL.HUB_DEVICE.concat(hubId);

			Hub hub = doGet(request, Hub.class);

			if (hub != null) {
				stats.put(HubInfoMetric.NAME.getName(), hub.getName());
				stats.put(HubInfoMetric.FIRMWARE_VERSION.getName(), hub.getFirmwareVersion());

				String locationName = cachedLocations.get(0).getName();
				Optional<Location> location = cachedLocations.stream().filter(l -> l.getLocationId().equals(locationIdFiltered)).findFirst();
				if (location.isPresent()) {
					locationName = location.get().getName();
				}
				stats.put(HubInfoMetric.CURRENT_LOCATION.getName(), locationName);
			} else {
				throw new ResourceNotReachableException("Hub information is empty");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveHubDetailInfo(stats, hubId, false);
			}
			if (this.logger.isErrorEnabled()) {
				logger.error("Error while retrieve hub info: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * This method is used to retrieve hub health info by send GET request to "https://api.smartthings.com/v1/hubdevices/{hubId}/health"
	 *
	 * @param stats store all statistics
	 * @param hubId id of SmartThings hub
	 * @param retryOnError retry if any error occurs
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveHubHealth(Map<String, String> stats, String hubId, boolean retryOnError) {
		try {
			String request = SmartThingsURL.DEVICES
					.concat(SmartThingsConstant.SLASH)
					.concat(hubId)
					.concat(SmartThingsURL.DEVICE_HEALTH);

			Hub hub = doGet(request, Hub.class);

			if (hub != null) {
				stats.put(HubInfoMetric.STATE.getName(), hub.getState());
			} else {
				throw new ResourceNotReachableException("Hub health info is empty");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveHubHealth(stats, hubId, false);
			}
			if (logger.isErrorEnabled()) {
				logger.error("Error while retrieve hub info: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * This method is used to retrieve locations info by send GET request to "https://api.smartthings.com/v1/locations"
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 * @param retryOnError retry if any error occurs
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveLocations(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, boolean retryOnError) {
		try {
			String request = SmartThingsURL.LOCATIONS;

			LocationWrapper locationWrapper = doGet(request, LocationWrapper.class);

			if (locationWrapper != null && !locationWrapper.getLocations().isEmpty()) {
				cachedLocations = locationWrapper.getLocations();
				locationIdFiltered = findLocationByName(locationFilter).getLocationId();
				populateLocationsManagement(stats, advancedControllableProperties);
			} else {
				throw new ResourceNotReachableException("Hub locations is empty");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveLocations(stats, advancedControllableProperties, false);
			}
			if (logger.isErrorEnabled()) {
				logger.error("Error while retrieve locations info: " + e.getMessage(), e);
			}

		}
	}

	/**
	 * This method is used to populate locations management group
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 */
	public void populateLocationsManagement(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		for (int locationIndex = 0; locationIndex < cachedLocations.size(); locationIndex++) {
			addAdvanceControlProperties(advancedControllableProperties, createText(stats, AggregatorGroupControllingMetric.LOCATION_MANAGEMENT.getName()
					+ LocationManagementMetric.LOCATION.getName() + locationIndex, cachedLocations.get(locationIndex).getName()));
		}
	}

	/**
	 * This method is used to retrieve rooms info by send GET request to "https://api.smartthings.com/v1/locations/{locationId}/rooms"
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 * @param retryOnError retry if any error occurs
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveRooms(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, boolean retryOnError) {
		try {
			String request = SmartThingsURL.LOCATIONS
					.concat(SmartThingsConstant.SLASH)
					.concat(locationIdFiltered)
					.concat(SmartThingsConstant.SLASH)
					.concat(SmartThingsURL.ROOMS);

			RoomWrapper roomWrapper = doGet(request, RoomWrapper.class);

			if (roomWrapper != null && !roomWrapper.getRooms().isEmpty()) {
				cachedRooms = roomWrapper.getRooms();
				populateRoomManagement(stats, advancedControllableProperties);
			} else {
				throw new ResourceNotReachableException("rooms is empty");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveRooms(stats, advancedControllableProperties, false);
			}
			if (logger.isErrorEnabled()) {
				logger.error("Error while retrieve rooms info: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * This method is used to populate locations management group
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 */
	public void populateRoomManagement(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		for (int roomIndex = 0; roomIndex < cachedRooms.size(); roomIndex++) {
			addAdvanceControlProperties(advancedControllableProperties,
					createText(stats, AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + roomIndex, cachedRooms.get(roomIndex).getName()));
			addAdvanceControlProperties(advancedControllableProperties,
					createButton(stats, AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + roomIndex + RoomManagementMetric.DELETE_ROOM.getName(),
							SmartThingsConstant.DELETE, SmartThingsConstant.DELETING));
		}
	}

	/**
	 * This method is used to populate locations management group
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 */
	public void populateCreateRoomManagement(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		Location location = findLocationByName(locationFilter);
		stats.put(AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.LOCATION.getName(), getDefaultValueForNullData(location.getName(), SmartThingsConstant.EMPTY));
		addAdvanceControlProperties(advancedControllableProperties,
				createText(stats, AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.ROOM_NAME.getName(), cachedCreateRoom.getName()));
		addAdvanceControlProperties(advancedControllableProperties,
				createButton(stats, AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.CREATE_ROOM.getName(), SmartThingsConstant.CREATE, SmartThingsConstant.CREATING));
		if (isEditedForCreateRoom.booleanValue()) {
			stats.put(AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.EDITED.getName(), isEditedForCreateRoom.toString());
			addAdvanceControlProperties(advancedControllableProperties,
					createButton(stats, AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.CANCEL.getName(), SmartThingsConstant.CANCEL, SmartThingsConstant.CANCELING));
		}
	}

	/**
	 * This method is used to retrieve scenes info by send GET request to "https://api.smartthings.com/v1/scene?locationId={locationId}"
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 * @param retryOnError retry if any error occurs
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveScenes(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, boolean retryOnError) {
		try {
			String request = SmartThingsURL.SCENE
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.LOCATION_ID)
					.concat(locationIdFiltered);

			SceneWrapper sceneWrapper = doGet(request, SceneWrapper.class);

			if (sceneWrapper != null && !sceneWrapper.getScenes().isEmpty()) {
				cachedScenes = sceneWrapper.getScenes();
				populateScenesManagement(stats, advancedControllableProperties);
			} else {
				throw new ResourceNotReachableException("rooms is empty");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveScenes(stats, advancedControllableProperties, false);
			}
			if (logger.isErrorEnabled()) {
				logger.error("Error while retrieve rooms info: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * This method is used to populate scenes trigger group
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 */
	public void populateScenesManagement(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		for (Scene scene : cachedScenes) {
			addAdvanceControlProperties(advancedControllableProperties,
					createButton(stats, AggregatorGroupControllingMetric.SCENE.getName().concat(scene.getSceneName()), SmartThingsConstant.RUN, SmartThingsConstant.RUNNING));
		}
	}
	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region retrieve aggregated devices info: locations, aggregated device and device capabilities info in worker thread
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Retrieve aggregated devices and system information data -
	 * and set next device/system collection iteration timestamp
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 */
	private void retrieveInfo(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		retrieveLocations(stats, advancedControllableProperties, true);
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("New fetched location list: %s", cachedLocations));
		}
		retrieveRooms(stats, advancedControllableProperties, true);
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("New fetched room list: %s", cachedRooms));
		}
		retrieveDevices(true);
	}

	/**
	 * This method is used to retrieve list of device every 30 seconds by send GET request to https://api.smartthings.com/v1/devices?locationId={locationId}
	 *
	 * @param retryOnError retry if any error occurs
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	private void retrieveDevices(boolean retryOnError) {
		try {
			String request = SmartThingsURL.DEVICES
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.LOCATION_ID)
					.concat(locationIdFiltered);

			DeviceWrapper responseDeviceList = this.doGet(request, DeviceWrapper.class);
			cachedDevices = (ConcurrentHashMap<String, Device>) responseDeviceList.getDevices().stream().collect(Collectors.toConcurrentMap(Device::getDeviceId, Function.identity()));

		} catch (Exception e) {
			if (retryOnError) {
				retrieveDevices(false);
			}
			if (logger.isErrorEnabled()) {
				logger.error(String.format("Aggregated Device Data Retrieval-Error: %s with cause: %s", e.getMessage(), e.getCause().getMessage()), e);
			}
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region perform aggregator control: location, room, create room, scene
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used for calling control location properties:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param value value of controllable property
	 * @throws ResourceNotReachableException when fail to control
	 */
	private void locationControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, String value) {
		try {
			int locationIndex = Integer.parseInt(controllableProperty.substring(LocationManagementMetric.LOCATION.getName().length()));
			for (int i = 0; i < cachedLocations.size(); i++) {
				if (i == locationIndex) {
					continue;
				}
				if (cachedLocations.get(i).getName().equalsIgnoreCase(value)) {
					throw new ResourceNotReachableException(String.format("The location name %s already exists, Please chose the different location name", value));
				}
			}
			String request = SmartThingsURL.LOCATIONS
					.concat(SmartThingsConstant.SLASH)
					.concat(cachedLocations.get(locationIndex).getLocationId());

			HttpHeaders headers = new HttpHeaders();

			Location location = new Location();
			location.setName(value);
			String requestBody = location.contributeRequestBody();

			ResponseEntity<?> response = doRequest(request, HttpMethod.PUT, headers, requestBody, String.class);

			handleRateLimitExceed(response);

			Optional<?> responseBody = Optional.ofNullable(response)
					.map(HttpEntity::getBody);
			if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
				location = objectMapper.readValue(responseBody.get().toString(), Location.class);
				cachedLocations.set(locationIndex, location);
			} else {
				throw new ResourceNotReachableException(String.format("Changing %s name fail, please try again later", controllableProperty));
			}

			addAdvanceControlProperties(advancedControllableProperties,
					createText(stats, AggregatorGroupControllingMetric.LOCATION_MANAGEMENT.getName() + LocationManagementMetric.LOCATION.getName() + locationIndex, value));
			isEmergencyDelivery = true;
		} catch (Exception e) {
			throw new ResourceNotReachableException(String.format("Error while controlling location %s: %s", controllableProperty, e.getMessage()), e);
		}
	}

	/**
	 * This method is used for calling control room properties:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param value value of controllable property
	 * @throws ResourceNotReachableException when fail to control
	 */
	private void roomControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, String value) {
		try {
			int roomIndex = Integer.parseInt(controllableProperty.substring(RoomManagementMetric.ROOM.getName().length(), RoomManagementMetric.ROOM.getName().length() + 1));
			for (int i = 0; i < cachedRooms.size(); i++) {
				if (i == roomIndex) {
					continue;
				}
				if (cachedRooms.get(i).getName().equalsIgnoreCase(value)) {
					throw new ResourceNotReachableException(String.format("The room name %s already exists, Please chose the different room name", value));
				}
			}

			RoomManagementMetric roomManagementMetric = RoomManagementMetric.getByName(controllableProperty, roomIndex);

			String request = SmartThingsURL.LOCATIONS
					.concat(SmartThingsConstant.SLASH)
					.concat(locationIdFiltered)
					.concat(SmartThingsConstant.SLASH)
					.concat(SmartThingsURL.ROOMS)
					.concat(SmartThingsConstant.SLASH)
					.concat(cachedRooms.get(roomIndex).getRoomId());

			HttpHeaders headers = new HttpHeaders();
			Room room = new Room();
			room.setName(value);
			String requestBody = room.contributeRequestBody();

			switch (roomManagementMetric) {
				case ROOM:
					ResponseEntity<?> response = doRequest(request, HttpMethod.PUT, headers, requestBody, String.class);

					handleRateLimitExceed(response);

					Optional<?> responseBody = Optional.ofNullable(response)
							.map(HttpEntity::getBody);
					if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
						room = objectMapper.readValue(responseBody.get().toString(), Room.class);
						cachedRooms.set(roomIndex, room);
					} else {
						throw new ResourceNotReachableException(String.format("Changing %s name fail, please try again later", controllableProperty));
					}

					addAdvanceControlProperties(advancedControllableProperties,
							createText(stats, AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + roomIndex, value));
					isEmergencyDelivery = true;
					break;
				case DELETE_ROOM:
					response = doRequest(request, HttpMethod.DELETE, headers, requestBody, String.class);

					handleRateLimitExceed(response);

					if (!response.getStatusCode().is2xxSuccessful()) {
						throw new ResourceNotReachableException(String.format("%s fail, please try again later", controllableProperty));
					}
					break;
				default:
					if (logger.isWarnEnabled()) {
						logger.warn(String.format("Operation %s is not supported.", controllableProperty));
					}
					throw new IllegalStateException(String.format("Operation %s is not supported.", controllableProperty));
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException(String.format("Error while controlling room %s: %s", controllableProperty, e.getMessage()), e);
		}
	}

	/**
	 * This method is used for calling control room properties:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @throws ResourceNotReachableException when fail to control
	 */
	private void sceneControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty) {
		try {
			String sceneId = findSceneIdByName(controllableProperty);
			String request = SmartThingsURL.SCENE
					.concat(SmartThingsConstant.SLASH)
					.concat(sceneId)
					.concat(SmartThingsURL.EXECUTE);

			ResponseEntity<?> response = doPost(request, String.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new ResourceNotReachableException(String.format("Run Scene %s fail, please try again later", controllableProperty));
			}
			addAdvanceControlProperties(advancedControllableProperties,
					createButton(stats, AggregatorGroupControllingMetric.SCENE.getName().concat(controllableProperty), SmartThingsConstant.SUCCESSFUL, SmartThingsConstant.SUCCESSFUL));
			isEmergencyDelivery = true;
		} catch (Exception e) {
			throw new ResourceNotReachableException(String.format("Error while controlling scene %s: %s", controllableProperty, e.getMessage()), e);
		}
	}

	/**
	 * This method is used for calling control create room properties:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param value value of controllable property
	 * @throws ResourceNotReachableException when fail to control
	 */
	private void createRoomControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, String value) {
		CreateRoomMetric createRoomMetric = CreateRoomMetric.getByName(controllableProperty);
		cachedCreateRoom.setLocationId(locationIdFiltered);

		isEditedForCreateRoom = true;
		switch (createRoomMetric) {
			case ROOM_NAME:
				for (Room room : cachedRooms) {
					if (room.getName().equalsIgnoreCase(value)) {
						throw new ResourceNotReachableException(String.format("The room name %s already exists, Please chose the different room name", value));
					}
				}
				cachedCreateRoom.setName(value);
				populateCreateRoomManagement(stats, advancedControllableProperties);
				break;
			case CANCEL:
				cachedCreateRoom = new Room();
				break;
			case CREATE_ROOM:
				isEditedForCreateRoom = false;
				try {
					String request = SmartThingsURL.LOCATIONS
							.concat(SmartThingsConstant.SLASH)
							.concat(locationIdFiltered)
							.concat(SmartThingsConstant.SLASH)
							.concat(SmartThingsURL.ROOMS);

					HttpHeaders headers = new HttpHeaders();
					String requestBody = cachedCreateRoom.contributeRequestBody();

					ResponseEntity<?> response = doRequest(request, HttpMethod.POST, headers, requestBody, String.class);

					handleRateLimitExceed(response);

					Optional<?> responseBody = Optional.ofNullable(response)
							.map(HttpEntity::getBody);
					if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
						Room room = objectMapper.readValue(responseBody.get().toString(), Room.class);
						cachedCreateRoom = new Room();
						cachedRooms.add(room);
					} else {
						throw new ResourceNotReachableException(String.format("Creating room with name %s fail, please try again later", value));
					}

					populateCreateRoomManagement(stats, advancedControllableProperties);
					populateRoomManagement(stats, advancedControllableProperties);
					isEmergencyDelivery = true;
				} catch (Exception e) {
					throw new ResourceNotReachableException(String.format("Error while controlling create room %s: %s", controllableProperty, e.getMessage()), e);
				}
				break;
			default:
				if (logger.isWarnEnabled()) {
					logger.warn(String.format("Operation %s is not supported.", controllableProperty));
				}
				throw new IllegalStateException(String.format("Operation %s is not supported.", controllableProperty));
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region populate device dashboard
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used to populate scenes trigger group
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 */
	public void populateDeviceView(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		for (Device device : cachedDevicesAfterPollingInterval.values()) {
			Optional<List<DetailViewPresentation>> actions = Optional.ofNullable(device.getPresentation())
					.map(DevicePresentation::getDashboardPresentations)
					.map(DashboardPresentation::getActions);

			if (actions.isPresent() && !actions.get().isEmpty()) {
				DetailViewPresentation action = actions.get().get(0);
				switch (action.getDisplayType()) {
					case DeviceDisplayTypesMetric.STAND_BY_POWER_SWITCH:
					case DeviceDisplayTypesMetric.TOGGLE_SWITCH:
					case DeviceDisplayTypesMetric.SWITCH:
						String onLabel = getDefaultValueForNullData(action.getStandbyPowerSwitch().getCommand().getOn(), SmartThingsConstant.ON);
						String offLabel = getDefaultValueForNullData(action.getStandbyPowerSwitch().getCommand().getOff(), SmartThingsConstant.OFF);
						String currentVale = getDefaultValueForNullData(action.getStandbyPowerSwitch().getValue(), SmartThingsConstant.OFF);
						addAdvanceControlProperties(advancedControllableProperties,
								createSwitch(stats, AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + device.getName(), currentVale, offLabel, onLabel));

						break;
					case DeviceDisplayTypesMetric.PUSH_BUTTON:
						addAdvanceControlProperties(advancedControllableProperties,
								createButton(stats, AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + device.getName(), SmartThingsConstant.PUSH, SmartThingsConstant.PUSHING));
						break;
					default:
						if (logger.isWarnEnabled()) {
							logger.warn(String.format("Unexpected device display type: %s", action.getDisplayType()));
						}
						break;
				}
			}
		}
	}

	/**
	 * This method is used for calling control device dashboard properties:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param value value of controllable property
	 * @throws ResourceNotReachableException when fail to control
	 */
	private void deviceDashboardControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, String value) {
		try {
			Device device = findDeviceByName(controllableProperty);
			if(device != null) {
				String request = SmartThingsURL.DEVICES
						.concat(SmartThingsConstant.SLASH)
						.concat(device.getDeviceId())
						.concat(SmartThingsConstant.SLASH)
						.concat(SmartThingsURL.COMMANDS);

				HttpHeaders headers = new HttpHeaders();

				Optional<List<DetailViewPresentation>> actions = Optional.ofNullable(device.getPresentation())
						.map(DevicePresentation::getDashboardPresentations)
						.map(DashboardPresentation::getActions);
				String command = SmartThingsConstant.EMPTY;
				String requestBody = SmartThingsConstant.EMPTY;

				if (actions.isPresent() && !actions.get().isEmpty()) {
					DetailViewPresentation action = actions.get().get(0);
					switch (action.getDisplayType()) {
						case DeviceDisplayTypesMetric.STAND_BY_POWER_SWITCH:
						case DeviceDisplayTypesMetric.TOGGLE_SWITCH:
						case DeviceDisplayTypesMetric.SWITCH:
							command = value.equals("1") ? action.getStandbyPowerSwitch().getCommand().getOn() : action.getStandbyPowerSwitch().getCommand().getOff();
							break;
						case DeviceDisplayTypesMetric.PUSH_BUTTON:
							command = action.getPushButton().getCommand();
							break;
						default:
							if (logger.isWarnEnabled()) {
								logger.warn(String.format("Unexpected device display type: %s", action.getDisplayType()));
							}
							break;
					}
					requestBody = device.contributeRequestBody(action.getCapability(), command);
				}

				ResponseEntity<?> response = doRequest(request, HttpMethod.POST, headers, requestBody, String.class);

				handleRateLimitExceed(response);

				Optional<?> responseBody = Optional.ofNullable(response)
						.map(HttpEntity::getBody);
				if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
					if (actions.isPresent() && actions.get().get(0).getDisplayType().toUpperCase().contains(DeviceDisplayTypesMetric.SWITCH.toUpperCase())) {
						device.getPresentation().getDashboardPresentations().getActions().get(0).getStandbyPowerSwitch().setValue(command);
					}
				} else {
					throw new ResourceNotReachableException(String.format("control device %s fail, please try again later", controllableProperty));
				}

				populateDeviceView(stats, advancedControllableProperties);
				isEmergencyDelivery = true;
			}else {
				throw new ResourceNotReachableException(String.format("can not find device: %s", controllableProperty));
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException(String.format("Error while controlling device %s: %s", controllableProperty, e.getMessage()), e);
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region populate advanced controllable properties
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Add advancedControllableProperties if advancedControllableProperties different empty
	 *
	 * @param advancedControllableProperties advancedControllableProperties is the list that store all controllable properties
	 * @param property the property is item advancedControllableProperties
	 */
	private void addAdvanceControlProperties(List<AdvancedControllableProperty> advancedControllableProperties, AdvancedControllableProperty property) {
		if (property != null) {
			for (AdvancedControllableProperty controllableProperty : advancedControllableProperties) {
				if (controllableProperty.getName().equals(property.getName())) {
					advancedControllableProperties.remove(controllableProperty);
					break;
				}
			}
			advancedControllableProperties.add(property);
		}
	}

	/**
	 * Create a controllable property Text
	 *
	 * @param name the name of property
	 * @param stringValue character string
	 * @return AdvancedControllableProperty Text instance
	 */
	private AdvancedControllableProperty createText(Map<String, String> stats, String name, String stringValue) {
		if (stringValue == null) {
			stringValue = SmartThingsConstant.EMPTY;
		}
		stats.put(name, stringValue);
		AdvancedControllableProperty.Text text = new AdvancedControllableProperty.Text();
		return new AdvancedControllableProperty(name, new Date(), text, stringValue);
	}

	/**
	 * Instantiate Text controllable property
	 *
	 * @param name name of the property
	 * @param label default button label
	 * @return AdvancedControllableProperty button instance
	 */
	private AdvancedControllableProperty createButton(Map<String, String> stats, String name, String label, String labelPressed) {
		AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
		stats.put(name, label);
		button.setLabel(label);
		button.setLabelPressed(labelPressed);
		button.setGracePeriod(0L);
		return new AdvancedControllableProperty(name, new Date(), button, "");
	}

	/**
	 * Create a switch controllable property
	 *
	 * @param name name of the switch
	 * @param status initial switch state (0|1)
	 * @return AdvancedControllableProperty button instance
	 */
	private AdvancedControllableProperty createSwitch(Map<String, String> stats, String name, String status, String labelOff, String labelOn) {
		AdvancedControllableProperty.Switch toggle = new AdvancedControllableProperty.Switch();
		toggle.setLabelOff(labelOff);
		toggle.setLabelOn(labelOn);

		int statusCode = 0;
		if (status.equalsIgnoreCase(labelOn)) {
			statusCode = 1;
		}
		stats.put(name, String.valueOf(statusCode));
		return new AdvancedControllableProperty(name, new Date(), toggle, statusCode);
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	/**
	 * calculating minimum of pooling interval
	 *
	 * @throws ResourceNotReachableException when get limit rate exceed error
	 */
	private int calculatingLocalPoolingInterval() {

		try {
			int pollingIntervalValue = SmartThingsConstant.MIN_POOLING_INTERVAL;
			if (StringUtils.isNotNullOrEmpty(poolingInterval)) {
				pollingIntervalValue = Integer.parseInt(poolingInterval);
			}

			int minPollingInterval = calculatingMinPoolingInterval();
			if (pollingIntervalValue < minPollingInterval) {
				if (logger.isErrorEnabled()) {
					logger.error(String.format("invalid pollingInterval value, pollingInterval must greater than: %s", minPollingInterval));
				}
				return minPollingInterval;
			}
			return pollingIntervalValue;
		} catch (Exception e) {
			throw new ResourceNotReachableException(String.format("Unexpected pollingInterval value: %s", poolingInterval));
		}
	}

	/**
	 * calculating minimum of pooling interval
	 *
	 * @throws ResourceNotReachableException when get limit rate exceed error
	 */
	private int calculatingMinPoolingInterval() {
		if (!cachedDevices.isEmpty()) {
			return IntMath.divide(cachedDevices.size()
					, SmartThingsConstant.MAX_THREAD_QUANTITY * SmartThingsConstant.MAX_DEVICE_QUANTITY_PER_THREAD
					, RoundingMode.CEILING);
		}
		return SmartThingsConstant.MIN_POOLING_INTERVAL;
	}


	/**
	 * calculating minimum of pooling interval
	 *
	 * @throws ResourceNotReachableException when get limit rate exceed error
	 */
	private int calculatingThreadQuantity() {
		if (cachedDevices.isEmpty()) {
			return SmartThingsConstant.MIN_THREAD_QUANTITY;
		}
		if (cachedDevices.size() / localPollingInterval < SmartThingsConstant.MAX_THREAD_QUANTITY * SmartThingsConstant.MAX_DEVICE_QUANTITY_PER_THREAD) {
			return IntMath.divide(cachedDevices.size(), localPollingInterval * SmartThingsConstant.MAX_DEVICE_QUANTITY_PER_THREAD, RoundingMode.CEILING);
		}
		return SmartThingsConstant.MAX_THREAD_QUANTITY;
	}

	/**
	 * Handle rate limit exceed error while controlling
	 *
	 * @param response ResponseEntity
	 * @throws ResourceNotReachableException when get limit rate exceed error
	 */
	private void handleRateLimitExceed(ResponseEntity<?> response) {
		Optional<String> rateLimit = Optional.ofNullable(response)
				.map(HttpEntity::getHeaders)
				.map(l -> l.get(SmartThingsConstant.RATE_LIMIT_HEADER_KEY))
				.map(t -> t.get(0));
		if (response.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS) && rateLimit.isPresent()) {
			Integer resetTime = Integer.parseInt(rateLimit.get()) / 1000;
			throw new ResourceNotReachableException(String.format("Rate limit exceeded; request rejected. please waiting for %s", resetTime));
		}
	}

	/**
	 * Check API token validation
	 *
	 * @return boolean
	 */
	private boolean validateApiToken() {
		return !StringUtils.isNullOrEmpty(apiToken);
	}

	/**
	 * Find device ID by device Type
	 *
	 * @param category device category
	 * @return String deviceId
	 */
	private String findDeviceIdByCategory(String category) {
		for (Device device : cachedDevices.values()) {
			String deviceCategory = device.retrieveCategory();
			if (StringUtils.isNotNullOrEmpty(category) && StringUtils.isNotNullOrEmpty(deviceCategory) && category.equals(deviceCategory)) {
				return device.getDeviceId();
			}
		}
		return SmartThingsConstant.EMPTY;
	}

	/**
	 * Find device by device Name
	 *
	 * @param name device name
	 * @return Device device info
	 */
	private Device findDeviceByName(String name) {
		for (Device device : cachedDevices.values()) {
			if (StringUtils.isNotNullOrEmpty(name) && name.equals(device.getName())) {
				return device;
			}
		}
		return null;
	}

	/**
	 * Find locationId by location name
	 *
	 * @param name location name
	 * @return String locationId
	 */
	private Location findLocationByName(String name) {
		Objects.requireNonNull(cachedLocations);
		if (StringUtils.isNotNullOrEmpty(name)) {
			Optional<Location> location = cachedLocations.stream().filter(l -> name.equals(l.getName())).findFirst();
			if (location.isPresent()) {
				return location.get();
			}
		}
		return cachedLocations.get(0);
	}

	/**
	 * Find sceneId by name
	 *
	 * @param name scene name
	 * @return String sceneId
	 */
	private String findSceneIdByName(String name) {
		Objects.requireNonNull(name);
		if (StringUtils.isNotNullOrEmpty(name)) {
			Optional<Scene> scene = cachedScenes.stream().filter(sc -> name.equals(sc.getSceneName())).findFirst();
			if (scene.isPresent()) {
				return scene.get().getSceneId();
			}
		}
		return SmartThingsConstant.EMPTY;
	}

	/**
	 * get default value for null data
	 *
	 * @param value value of monitoring properties
	 * @param defaultValue default value of monitoring properties
	 * @return String (none/value)
	 */
	private String getDefaultValueForNullData(String value, String defaultValue) {
		return StringUtils.isNullOrEmpty(value) ? defaultValue : value;
	}

	/**
	 * push failed monitoring Device ID to priority in next poolingInterval
	 *
	 * @return String (none/value)
	 */
	private List<String> pushFailedMonitoringDevicesIDToPriority() {
		if (failedMonitoringDeviceIds.isEmpty()) {
			return cachedDevices.values().stream().map(Device::getDeviceId).collect(Collectors.toList());
		}
		List<String> deviceIdList = cachedDevices.values().stream().map(Device::getDeviceId).filter(id -> !failedMonitoringDeviceIds.contains(id)).collect(Collectors.toList());
		deviceIds.addAll(failedMonitoringDeviceIds);
		failedMonitoringDeviceIds.clear();
		return deviceIdList;
	}

}
