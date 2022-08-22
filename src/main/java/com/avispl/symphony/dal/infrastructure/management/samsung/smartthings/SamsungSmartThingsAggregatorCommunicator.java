package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings;

import java.math.RoundingMode;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 * SamsungSmartThingsAggregatorCommunicator
 * An implementation of RestCommunicator to provide communication and interaction with SmartThings cloud and its aggregated devices
 * Supported aggregated device categories are:
 * <li>Light</li>
 * <li>Presence Sensor</li>
 * <li>Thermos stats</li>
 * <li>Window Shade</li>
 * <li>Television</li>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/22/2022
 * @since 1.0.0
 */
public class SamsungSmartThingsAggregatorCommunicator extends RestCommunicator implements Aggregator, Monitorable, Controller {

	/**
	 * Process is running constantly and triggers collecting data from SmartThings API endpoints base on getMultipleStatistic
	 *
	 * @author Kevin
	 * @since 1.0.0
	 */
	class SamsungSmartThingsDeviceDataLoader implements Runnable {
		private volatile int threadIndex;

		/**
		 * Parameters constructors
		 *
		 * @param threadIndex index of thread
		 */
		public SamsungSmartThingsDeviceDataLoader(int threadIndex) {
			this.threadIndex = threadIndex;
		}

		@Override
		public void run() {
			if (logger.isDebugEnabled()) {
				logger.debug("Fetching other than SmartThings device list" + threadIndex);
			}
			if (!cachedDevices.isEmpty()) {
				retrieveDeviceDetail(threadIndex);
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
	 * @param currentThread current thread index
	 *
	 * Submit thread to get device detail info
	 */
	private void retrieveDeviceDetail(int currentThread) {
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
				retrieveDevicePresentation(deviceId);
				retrieveDeviceFullStatus(deviceId);

				mapDevicesToAggregatedDevice(cachedDevices.get(deviceId));
				if (logger.isDebugEnabled()) {
					Long time = System.currentTimeMillis() - startTime;
					logger.debug(String.format("Finished fetch %s details info in worker thread: %s", cachedDevices.get(deviceId).getName(), time));
				}
			}
		} catch (Exception e) {
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
				throw new ResourceNotReachableException(String.format("%s health info is empty", cachedDevicesAfterPollingInterval.get(deviceID).getName()));
			}
		} catch (Exception e) {
			failedMonitoringDeviceIds.add(deviceID);
			if (logger.isErrorEnabled()) {
				logger.error(String.format("Error while retrieve %s health info: %s ", cachedDevicesAfterPollingInterval.get(deviceID).getName(), e.getMessage()), e);
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
				cachedPresentations.put(deviceId, devicePresentation);
				cachedDevices.get(deviceId).setPresentation(devicePresentation);
			} else if (cachedPresentations.get(deviceId) != null) {
				DevicePresentation cachedPresentation = cachedPresentations.get(deviceId);
				cachedDevices.get(deviceId).setPresentation(cachedPresentation);
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
				throw new ResourceNotReachableException(String.format("%s presentation info is empty", cachedDevicesAfterPollingInterval.get(deviceId).getName()));
			}
		} catch (Exception e) {
			failedMonitoringDeviceIds.add(deviceId);
			if (logger.isErrorEnabled()) {
				logger.error(String.format("Error while retrieve %s presentation info: %s", cachedDevicesAfterPollingInterval.get(deviceId).getName(), e.getMessage()), e);
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
				if (SmartThingsConstant.NONE.equals(value) || StringUtils.isNullOrEmpty(value)) {
					value = Optional.ofNullable(response.elements().next())
							.map(JsonNode::elements)
							.map(Iterator::next)
							.map(c -> c.get(detailViewPresentation.getCapability()))
							.map(d -> d.get(detailViewPresentation.getCapability()))
							.map(u -> u.get(SmartThingsConstant.VALUE))
							.map(JsonNode::asText)
							.orElse(SmartThingsConstant.NONE);
				}
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
	 * Number of threads in a thread pool reserved for the device statistics collection
	 */
	private volatile int deviceStatisticsCollectionThreads;

	/**
	 * SmartThings personal access token
	 */
	private String apiToken;

	/**
	 * SmartThings current room in device dashboard
	 */
	private String currentRoomInDeviceDashBoard;


	/**
	 * Caching the list of locations
	 */
	private List<Location> cachedLocations = new ArrayList<>();

	/**
	 * Caching the list rooms data
	 */
	private List<Room> cachedRooms = new ArrayList<>();

	/**
	 * Caching create room data
	 */
	private Room cachedCreateRoom = new Room();

	/**
	 * Caching the list of scenes data
	 */
	private List<Scene> cachedScenes = new ArrayList<>();

	/**
	 * Caching the list of devices data
	 */
	private ConcurrentHashMap<String, Device> cachedDevices = new ConcurrentHashMap<>();

	/**
	 * Caching the list of devices data
	 */
	private ConcurrentHashMap<String, DevicePresentation> cachedPresentations = new ConcurrentHashMap<>();

	/**
	 * Caching the list of devices data after polling interval
	 */
	private ConcurrentHashMap<String, Device> cachedDevicesAfterPollingInterval = new ConcurrentHashMap<>();

	/**
	 * Caching the list of device Ids
	 */
	private Set<String> deviceIds = ConcurrentHashMap.newKeySet();

	/**
	 * Caching the list of failed monitoring devices
	 */
	private Set<String> failedMonitoringDeviceIds = ConcurrentHashMap.newKeySet();

	/**
	 * List of aggregated devices
	 */
	private ConcurrentHashMap<String, AggregatedDevice> aggregatedDevices = new ConcurrentHashMap<>();

	/**
	 * Executor that runs all the async operations
	 */
	private static ExecutorService executorService;

	/**
	 * ReentrantLock to prevent null pointer exception to localExtendedStatistics when controlProperty method is called before GetMultipleStatistics method.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	// Adapter properties
	private String locationFilter;
	private String pollingInterval;
	private String deviceTypesFilter;
	private String deviceNamesFilter;
	private String roomsFilter;
	private String configManagement;

	private String locationIdFiltered;
	private ExtendedStatistics localExtendedStatistics;
	private boolean isEmergencyDelivery = false;
	private Boolean isEditedForCreateRoom = false;
	private ObjectMapper objectMapper = new ObjectMapper();
	private Set<String> unusedDeviceControlKeys = new HashSet<>();
	private Set<String> unusedRoomControlKeys = new HashSet<>();
	private boolean isConfigManagement;

	/**
	 * Polling interval which applied in adapter
	 */
	private volatile int localPollingInterval = SmartThingsConstant.MIN_POLLING_INTERVAL;

	/**
	 * The current phase of monitoring cycle in polling interval
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
	 * Retrieves {@code {@link #pollingInterval }}
	 *
	 * @return value of {@link #pollingInterval}
	 */
	public String getPollingInterval() {
		return pollingInterval;
	}

	/**
	 * Sets {@code pollingInterval}
	 *
	 * @param pollingInterval the {@code java.lang.String} field
	 */
	public void setPollingInterval(String pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	/**
	 * Retrieves {@code {@link #deviceTypesFilter}}
	 *
	 * @return value of {@link #deviceTypesFilter}
	 */
	public String getDeviceTypesFilter() {
		return deviceTypesFilter;
	}

	/**
	 * Sets {@code deviceTypesFilter}
	 *
	 * @param deviceTypesFilter the {@code java.lang.String} field
	 */
	public void setDeviceTypesFilter(String deviceTypesFilter) {
		this.deviceTypesFilter = deviceTypesFilter;
	}

	/**
	 * Retrieves {@code {@link #deviceNamesFilter }}
	 *
	 * @return value of {@link #deviceNamesFilter}
	 */
	public String getDeviceNamesFilter() {
		return deviceNamesFilter;
	}

	/**
	 * Sets {@code devicesNamesFilter}
	 *
	 * @param deviceNamesFilter the {@code java.lang.String} field
	 */
	public void setDeviceNamesFilter(String deviceNamesFilter) {
		this.deviceNamesFilter = deviceNamesFilter;
	}

	/**
	 * Retrieves {@code {@link #roomsFilter}}
	 *
	 * @return value of {@link #roomsFilter}
	 */
	public String getRoomsFilter() {
		return roomsFilter;
	}

	/**
	 * Sets {@code roomsFilter}
	 *
	 * @param roomsFilter the {@code java.lang.String} field
	 */
	public void setRoomsFilter(String roomsFilter) {
		this.roomsFilter = roomsFilter;
	}

	/**
	 * Retrieves {@code {@link #configManagement}}
	 *
	 * @return value of {@link #configManagement}
	 */
	public String getConfigManagement() {
		return configManagement;
	}

	/**
	 * Sets {@code configManagement}
	 *
	 * @param configManagement the {@code java.lang.String} field
	 */
	public void setConfigManagement(String configManagement) {
		this.configManagement = configManagement;
	}

	/**
	 * Retrieves {@code {@link #locationIdFiltered}}
	 *
	 * @return value of {@link #locationIdFiltered}
	 */
	public String getLocationIdFiltered() {
		return locationIdFiltered;
	}

	/**
	 * Sets {@code locationIdFiltered}
	 *
	 * @param locationIdFiltered the {@code java.lang.String} field
	 */
	public void setLocationIdFiltered(String locationIdFiltered) {
		this.locationIdFiltered = locationIdFiltered;
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
			logger.debug(String.format("Getting statistics from the Samsung SmartThings aggregator at host %s with port %s", this.host, this.getPort()));
			if (!validateApiToken()) {
				throw new ResourceNotReachableException("Personal access token cannot be null or empty, please enter valid token in the password field.");
			}
		}
		reentrantLock.lock();
		try {
			ExtendedStatistics extendedStatistics = new ExtendedStatistics();
			Map<String, String> stats = new HashMap<>();
			List<AdvancedControllableProperty> advancedControllableProperties = new ArrayList<>();
			isValidConfigManagement();

			if (!isEmergencyDelivery) {

				// retrieve device and filter devices in first monitoring cycle of polling interval
				if (currentPhase.get() == localPollingInterval || currentPhase.get() == 0) {
					cachedDevicesAfterPollingInterval = (ConcurrentHashMap<String, Device>) cachedDevices.values().stream().collect(Collectors.toConcurrentMap(Device::getDeviceId, Device::new));
					retrieveInfo();
					filterDeviceIds();
				}

				// populate edit location, create room, edit room group when configManagement is true
				if (isConfigManagement) {
					populateLocationsManagement(stats, advancedControllableProperties);
					populateRoomManagement(stats, advancedControllableProperties);
					populateCreateRoomManagement(stats, advancedControllableProperties);
				}
				retrieveScenes(true);
				populateScenesManagement(stats, advancedControllableProperties);

				// retrieve/ populate hub detail and submit threads to get devices when list of device is not empty
				if (!cachedDevices.isEmpty()) {
					String hubId = findDeviceIdByCategory(DeviceCategoriesMetric.HUB.getName());
					if (!hubId.isEmpty()) {
						retrieveHubDetailInfo(stats, hubId, true);
						retrieveHubHealth(stats, hubId, true);
					}
					populatePollingInterval(stats);

					// calculating polling interval and threads quantity
					if (currentPhase.get() == SmartThingsConstant.FIRST_MONITORING_CYCLE_OF_POLLING_INTERVAL) {
						localPollingInterval = calculatingLocalPollingInterval();
						deviceStatisticsCollectionThreads = calculatingThreadQuantity();
						pushFailedMonitoringDevicesIDToPriority();
					}

					if (currentPhase.get() == localPollingInterval) {
						currentPhase.set(0);
					}
					currentPhase.incrementAndGet();

					if (executorService == null) {
						executorService = Executors.newFixedThreadPool(deviceStatisticsCollectionThreads);
					}
					for (int threadNumber = 0; threadNumber < deviceStatisticsCollectionThreads; threadNumber++) {
						executorService.submit(new SamsungSmartThingsDeviceDataLoader(threadNumber));
					}
				}
				populateDeviceView(stats, advancedControllableProperties);
				extendedStatistics.setStatistics(stats);
				extendedStatistics.setControllableProperties(advancedControllableProperties);
				localExtendedStatistics = extendedStatistics;
				isEmergencyDelivery = false;
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
			if (executorService != null) {
				executorService.shutdownNow();
				executorService = null;
			}

			cachedRooms.clear();
			cachedDevices.clear();
			cachedDevicesAfterPollingInterval.clear();
			cachedLocations.clear();
			aggregatedDevices.clear();
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
	 * @param retryOnError retry if any error occurs
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveLocations(boolean retryOnError) {
		try {
			String request = SmartThingsURL.LOCATIONS;

			LocationWrapper locationWrapper = doGet(request, LocationWrapper.class);

			if (locationWrapper != null && !locationWrapper.getLocations().isEmpty()) {
				cachedLocations = locationWrapper.getLocations();
				locationIdFiltered = findLocationByName(locationFilter).getLocationId();
			} else {
				throw new ResourceNotReachableException("Hub locations is empty");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveLocations(false);
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
					+ LocationManagementMetric.LOCATION.getName() + formatOrderNumber(locationIndex, Arrays.asList(cachedLocations.toArray())), cachedLocations.get(locationIndex).getName()));
		}
		String locationName = cachedLocations.stream().filter(l -> l.getLocationId().equals(locationIdFiltered)).map(Location::getName).findFirst()
				.orElse(cachedLocations.get(0).getName());
		stats.put(HubInfoMetric.CURRENT_LOCATION.getName(), locationName);
	}

	/**
	 * This method is used to retrieve rooms info by send GET request to "https://api.smartthings.com/v1/locations/{locationId}/rooms"
	 *
	 * @param retryOnError retry if any error occurs
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveRooms(boolean retryOnError) {
		try {
			String request = SmartThingsURL.LOCATIONS
					.concat(SmartThingsConstant.SLASH)
					.concat(locationIdFiltered)
					.concat(SmartThingsConstant.SLASH)
					.concat(SmartThingsURL.ROOMS);

			RoomWrapper roomWrapper = doGet(request, RoomWrapper.class);

			if (roomWrapper != null && !roomWrapper.getRooms().isEmpty()) {
				cachedRooms = roomWrapper.getRooms();
			} else {
				throw new ResourceNotReachableException("rooms is empty");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveRooms(false);
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
		boolean isEmptyRoomControl = true;

		if (unusedRoomControlKeys != null) {
			removeUnusedStatsAndControls(stats, advancedControllableProperties, unusedRoomControlKeys);
		}

		for (int roomIndex = 0; roomIndex < cachedRooms.size(); roomIndex++) {
			String editRoomKey = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + formatOrderNumber(roomIndex, Arrays.asList(cachedRooms.toArray()));
			String deleteRoomKey = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + formatOrderNumber(roomIndex, Arrays.asList(cachedRooms.toArray()))
					+ RoomManagementMetric.DELETE_ROOM.getName();

			addAdvanceControlProperties(advancedControllableProperties, createText(stats, editRoomKey, cachedRooms.get(roomIndex).getName()));
			addAdvanceControlProperties(advancedControllableProperties, createButton(stats, deleteRoomKey, SmartThingsConstant.DELETE, SmartThingsConstant.DELETING));
			isEmptyRoomControl = false;

			unusedRoomControlKeys.add(editRoomKey);
			unusedRoomControlKeys.add(deleteRoomKey);
		}
		// populate message when location have no rooms after filtering
		if (isEmptyRoomControl) {
			stats.put(AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + AggregatorGroupControllingMetric.MESSAGE.getName(), "There is no room in this location");
		} else {
			stats.remove(AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + AggregatorGroupControllingMetric.MESSAGE.getName());
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
		if (!isEditedForCreateRoom.booleanValue() || StringUtils.isNullOrEmpty(cachedCreateRoom.getName())) {
			isEditedForCreateRoom = false;
			advancedControllableProperties.removeIf(
					advancedControllableProperty -> advancedControllableProperty.getName().equals(AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.CANCEL.getName()));
			stats.remove(AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.CANCEL.getName());
			stats.put(AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.EDITED.getName(), isEditedForCreateRoom.toString());
		}
	}

	/**
	 * This method is used to retrieve scenes info by send GET request to "https://api.smartthings.com/v1/scene?locationId={locationId}"
	 *
	 * @param retryOnError retry if any error occurs
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveScenes(boolean retryOnError) {
		try {
			String request = SmartThingsURL.SCENE
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.LOCATION_ID)
					.concat(locationIdFiltered);

			SceneWrapper sceneWrapper = doGet(request, SceneWrapper.class);

			if (sceneWrapper != null && !sceneWrapper.getScenes().isEmpty()) {
				cachedScenes = sceneWrapper.getScenes();
			} else {
				throw new ResourceNotReachableException("scenes is empty");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveScenes(false);
			}
			if (logger.isErrorEnabled()) {
				logger.error("Error while retrieve scene info: " + e.getMessage(), e);
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
		boolean isEmptyScenesControl = true;
		for (Scene scene : cachedScenes) {
			addAdvanceControlProperties(advancedControllableProperties,
					createButton(stats, AggregatorGroupControllingMetric.SCENE.getName().concat(scene.getSceneName()), SmartThingsConstant.RUN, SmartThingsConstant.RUNNING));
			isEmptyScenesControl = false;
		}
		// populate message when location have no scene after filtering
		if (isEmptyScenesControl) {
			stats.put(AggregatorGroupControllingMetric.SCENE.getName() + AggregatorGroupControllingMetric.MESSAGE.getName(), "There is no scene in this location");
		} else {
			stats.remove(AggregatorGroupControllingMetric.SCENE.getName() + AggregatorGroupControllingMetric.MESSAGE.getName());
		}
	}

	/**
	 * This method is used to populate polling interval
	 *
	 * @param stats store all statistics
	 */
	public void populatePollingInterval(Map<String, String> stats) {
		Integer minPollingInterval = calculatingMinPollingInterval();

		Long nextPollingInterval = System.currentTimeMillis() + localPollingInterval * 1000;
		Date date = new Date(nextPollingInterval);
		Format format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

		stats.put(HubInfoMetric.MIN_POLLING_INTERVAL.getName(), minPollingInterval.toString());
		stats.put(HubInfoMetric.NEXT_POLLING_INTERVAL.getName(), format.format(date));

	}

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region retrieve aggregated devices info: locations, aggregated device and device capabilities info in worker thread
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Retrieve aggregated devices and system information data -
	 * and set next device/system collection iteration timestamp
	 */
	private void retrieveInfo() {
		retrieveLocations(true);
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("New fetched location list: %s", cachedLocations));
		}
		retrieveRooms(true);
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("New fetched room list: %s", cachedRooms));
		}
		retrieveDevices(true);
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("New fetched device list: %s", cachedDevices));
		}
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
			int roomIndex = Integer.parseInt(controllableProperty.substring(RoomManagementMetric.ROOM.getName().length(),
					RoomManagementMetric.ROOM.getName().length() + String.valueOf(cachedRooms.size() - SmartThingsConstant.CONVERT_POSITION_TO_INDEX).length()));
			for (int i = 0; i < cachedRooms.size(); i++) {
				if (i == roomIndex) {
					continue;
				}
				if (cachedRooms.get(i).getName().equalsIgnoreCase(value)) {
					throw new ResourceNotReachableException(String.format("The room name %s already exists, Please chose the different room name", value));
				}
			}

			RoomManagementMetric roomManagementMetric = RoomManagementMetric.getByName(controllableProperty, formatOrderNumber(roomIndex, Arrays.asList(cachedRooms.toArray())));

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

					populateRoomManagement(stats, advancedControllableProperties);
					isEmergencyDelivery = true;
					break;
				case DELETE_ROOM:
					response = doRequest(request, HttpMethod.DELETE, headers, requestBody, String.class);

					handleRateLimitExceed(response);

					if (!response.getStatusCode().is2xxSuccessful()) {
						throw new ResourceNotReachableException(String.format("%s fail, please try again later", controllableProperty));
					}
					cachedRooms.remove(roomIndex);

					populateRoomManagement(stats, advancedControllableProperties);
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
				if (cachedRooms.size() >= SmartThingsConstant.MAX_ROOM_QUANTITY) {
					throw new ResourceNotReachableException(String.format("Can not create more than %s room", SmartThingsConstant.MAX_ROOM_QUANTITY));
				}
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
		Set<String> filteredRoom = convertUserInput(roomsFilter);
		List<String> rooms;

		// Get list room modes when applying filter
		if (StringUtils.isNotNullOrEmpty(roomsFilter)) {
			rooms = cachedRooms.stream().map(Room::getName).filter(filteredRoom::contains).collect(Collectors.toList());
			if (filteredRoom.contains(SmartThingsConstant.NO_ROOM_ASSIGNED)) {
				rooms.add(SmartThingsConstant.NO_ROOM_ASSIGNED);
			}
		} else {
			// Get list room modes when do not applying filter
			rooms = cachedRooms.stream().map(Room::getName).collect(Collectors.toList());
			rooms.add(SmartThingsConstant.NO_ROOM_ASSIGNED);
		}
		if (StringUtils.isNullOrEmpty(currentRoomInDeviceDashBoard) && !rooms.isEmpty()) {
			currentRoomInDeviceDashBoard = rooms.get(0);
		}
		String currentRoomId = findRoomIdByName(currentRoomInDeviceDashBoard);

		// remove unused control
		if (!unusedDeviceControlKeys.isEmpty()) {
			removeUnusedStatsAndControls(stats, advancedControllableProperties, unusedDeviceControlKeys);
		}
		boolean isEmptyDevicesControl = true;

		// populate ActiveRoom dropdown list control
		addAdvanceControlProperties(advancedControllableProperties,
				createDropdown(stats, AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + RoomManagementMetric.ACTIVE_ROOM.getName(), rooms, currentRoomInDeviceDashBoard));

		// populate device control base on active room
		for (Device device : cachedDevicesAfterPollingInterval.values()) {

			// filter device by value of ActiveRoom dropdown list
			String roomId = getDefaultValueForNullData(device.getRoomId(), SmartThingsConstant.EMPTY);
			if (roomId.equals(currentRoomId)) {
				Optional<List<DetailViewPresentation>> actions = Optional.ofNullable(device.getPresentation())
						.map(DevicePresentation::getDashboardPresentations)
						.map(DashboardPresentation::getActions);

				// populate control
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
							unusedDeviceControlKeys.add(AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + device.getName());
							isEmptyDevicesControl = false;
							break;
						case DeviceDisplayTypesMetric.PUSH_BUTTON:
							addAdvanceControlProperties(advancedControllableProperties,
									createButton(stats, AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + device.getName(), SmartThingsConstant.PUSH, SmartThingsConstant.PUSHING));
							unusedDeviceControlKeys.add(AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + device.getName());

							isEmptyDevicesControl = false;
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
		// populate message when room have no device after filtering
		if (isEmptyDevicesControl) {
			stats.put(AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + AggregatorGroupControllingMetric.MESSAGE.getName(), "There is no device in room or location");
		} else {
			stats.remove(AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + AggregatorGroupControllingMetric.MESSAGE.getName());
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

		// ActiveRoom dropdown control
		if (controllableProperty.equals(RoomManagementMetric.ACTIVE_ROOM.getName())) {
			currentRoomInDeviceDashBoard = value;
			populateDeviceView(stats, advancedControllableProperties);
		} else {
			// devices control
			try {
				Device device = findDeviceByName(controllableProperty);
				if (device != null) {
					String request = SmartThingsURL.DEVICES
							.concat(SmartThingsConstant.SLASH)
							.concat(device.getDeviceId())
							.concat(SmartThingsURL.COMMANDS);

					HttpHeaders headers = new HttpHeaders();

					// contribute request body
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

					// handle response when control device successful
					if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
						if (actions.isPresent() && !actions.get().isEmpty()) {
							DetailViewPresentation action = actions.get().get(0);
							switch (action.getDisplayType()) {
								case DeviceDisplayTypesMetric.STAND_BY_POWER_SWITCH:
								case DeviceDisplayTypesMetric.TOGGLE_SWITCH:
								case DeviceDisplayTypesMetric.SWITCH:
									String onLabel = getDefaultValueForNullData(action.getStandbyPowerSwitch().getCommand().getOn(), SmartThingsConstant.ON);
									String offLabel = getDefaultValueForNullData(action.getStandbyPowerSwitch().getCommand().getOff(), SmartThingsConstant.OFF);

									action.getStandbyPowerSwitch().setValue(command);
									device.getPresentation().getDashboardPresentations().getActions().set(0, action);
									cachedDevicesAfterPollingInterval.put(device.getDeviceId(), device);

									addAdvanceControlProperties(advancedControllableProperties,
											createSwitch(stats, AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + device.getName(), command, offLabel, onLabel));
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
					} else {
						throw new ResourceNotReachableException(String.format("control device %s fail, please try again later", controllableProperty));
					}

					populateDeviceView(stats, advancedControllableProperties);
					isEmergencyDelivery = true;
				} else {
					throw new ResourceNotReachableException(String.format("can not find device: %s", controllableProperty));
				}
			} catch (Exception e) {
				throw new ResourceNotReachableException(String.format("Error while controlling device %s: %s", controllableProperty, e.getMessage()), e);
			}
		}
	}
	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region filtering
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Filter By Category, Room, Name
	 */
	private void filterDeviceIds() {
		Set<String> filteredCategories = convertUserInput(deviceTypesFilter);
		Set<String> supportedCategories = Arrays.stream(DeviceCategoriesMetric.values())
				.filter(DeviceCategoriesMetric::isImplement)
				.map(DeviceCategoriesMetric::getName)
				.collect(Collectors.toSet());
		Set<String> filteredNames = convertUserInput(deviceNamesFilter);
		Set<String> filteredRooms = convertUserInput(roomsFilter);
		if (filteredRooms.contains(SmartThingsConstant.NO_ROOM_ASSIGNED)) {
			filteredRooms.remove(SmartThingsConstant.NO_ROOM_ASSIGNED);
			filteredRooms.add(SmartThingsConstant.EMPTY);
		}

		for (Device device : cachedDevices.values()) {
			if (!supportedCategories.contains(device.retrieveCategory())) {
				continue;
			}
			if (!filteredCategories.isEmpty() && !filteredCategories.contains(device.retrieveCategory())) {
				continue;
			}
			if (!filteredRooms.isEmpty() && !filteredRooms.contains(findRoomNameById(device.getRoomId()))) {
				continue;
			}
			if (!filteredNames.isEmpty() && !filteredNames.contains(device.getName())) {
				continue;
			}
			deviceIds.add(device.getDeviceId());
		}
	}

	/**
	 * This method is used to handle input from adapter properties and convert it to Set of String for control
	 *
	 * @return Set<String> is the Set of String of filter element
	 */
	public Set<String> convertUserInput(String input) {
		if (!StringUtils.isNullOrEmpty(input)) {
			String[] listAdapterPropertyElement = input.split(SmartThingsConstant.COMMA);

			// Remove start and end spaces of each adapterProperty
			Set<String> setAdapterPropertiesElement = new HashSet<>();
			for (String adapterPropertyElement : listAdapterPropertyElement) {
				setAdapterPropertiesElement.add(adapterPropertyElement.trim());
			}
			return setAdapterPropertiesElement;
		}
		return Collections.emptySet();
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

	/***
	 * Create AdvancedControllableProperty preset instance
	 * @param name name of the control
	 * @param initialValue initial value of the control
	 * @return AdvancedControllableProperty preset instance
	 */
	private AdvancedControllableProperty createDropdown(Map<String, String> stats, String name, List<String> values, String initialValue) {
		stats.put(name, initialValue);
		AdvancedControllableProperty.DropDown dropDown = new AdvancedControllableProperty.DropDown();
		dropDown.setOptions(values.toArray(new String[0]));
		dropDown.setLabels(values.toArray(new String[0]));

		return new AdvancedControllableProperty(name, new Date(), dropDown, initialValue);
	}

	/**
	 * This method is used to remove unused statistics/AdvancedControllableProperty from {@link SamsungSmartThingsAggregatorCommunicator#localExtendedStatistics}
	 *
	 * @param stats Map of statistics that contains statistics to be removed
	 * @param controls Set of controls that contains AdvancedControllableProperty to be removed
	 * @param listKeys list key of statistics to be removed
	 */
	private void removeUnusedStatsAndControls(Map<String, String> stats, List<AdvancedControllableProperty> controls, Set<String> listKeys) {
		for (String key : listKeys) {
			stats.remove(key);
			controls.removeIf(advancedControllableProperty -> advancedControllableProperty.getName().equals(key));
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	/**
	 * calculating minimum of polling interval
	 *
	 * @throws ResourceNotReachableException when get limit rate exceed error
	 */
	private int calculatingLocalPollingInterval() {

		try {
			int pollingIntervalValue = SmartThingsConstant.MIN_POLLING_INTERVAL;
			if (StringUtils.isNotNullOrEmpty(pollingInterval)) {
				pollingIntervalValue = Integer.parseInt(pollingInterval);
			}

			int minPollingInterval = calculatingMinPollingInterval();
			if (pollingIntervalValue < minPollingInterval) {
				if (logger.isErrorEnabled()) {
					logger.error(String.format("invalid pollingInterval value, pollingInterval must greater than: %s", minPollingInterval));
				}
				return minPollingInterval;
			}
			return pollingIntervalValue;
		} catch (Exception e) {
			throw new ResourceNotReachableException(String.format("Unexpected pollingInterval value: %s", pollingInterval));
		}
	}

	/**
	 * calculating minimum of polling interval
	 *
	 * @throws ResourceNotReachableException when get limit rate exceed error
	 */
	private int calculatingMinPollingInterval() {
		if (!deviceIds.isEmpty()) {
			return IntMath.divide(deviceIds.size()
					, SmartThingsConstant.MAX_THREAD_QUANTITY * SmartThingsConstant.MAX_DEVICE_QUANTITY_PER_THREAD
					, RoundingMode.CEILING);
		}
		return SmartThingsConstant.MIN_POLLING_INTERVAL;
	}


	/**
	 * calculating minimum of polling interval
	 *
	 * @throws ResourceNotReachableException when get limit rate exceed error
	 */
	private int calculatingThreadQuantity() {
		if (deviceIds.isEmpty()) {
			return SmartThingsConstant.MIN_THREAD_QUANTITY;
		}
		if (deviceIds.size() / localPollingInterval < SmartThingsConstant.MAX_THREAD_QUANTITY * SmartThingsConstant.MAX_DEVICE_QUANTITY_PER_THREAD) {
			return IntMath.divide(deviceIds.size(), localPollingInterval * SmartThingsConstant.MAX_DEVICE_QUANTITY_PER_THREAD, RoundingMode.CEILING);
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
		for (Device device : cachedDevicesAfterPollingInterval.values()) {
			if (StringUtils.isNotNullOrEmpty(name) && name.equals(device.getName())) {
				return device;
			}
		}
		return null;
	}

	/**
	 * Find location by location name
	 *
	 * @param name location name
	 * @return String location
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
	 * Find room name by room name
	 *
	 * @param id Room ID
	 * @return String Room năm
	 */
	private String findRoomNameById(String id) {
		Objects.requireNonNull(cachedRooms);
		if (StringUtils.isNotNullOrEmpty(id)) {
			Optional<Room> room = cachedRooms.stream().filter(r -> id.equals(r.getRoomId())).findFirst();
			if (room.isPresent()) {
				return room.get().getName();
			}
		}
		return SmartThingsConstant.EMPTY;
	}

	/**
	 * Find room name by room name
	 *
	 * @param name Room ID
	 * @return String Room năm
	 */
	private String findRoomIdByName(String name) {
		Objects.requireNonNull(cachedRooms);
		if (StringUtils.isNotNullOrEmpty(name)) {
			Optional<Room> room = cachedRooms.stream().filter(r -> name.equals(r.getName())).findFirst();
			if (room.isPresent()) {
				return room.get().getRoomId();
			}
		}
		return SmartThingsConstant.EMPTY;
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
	 * push failed monitoring Device ID to priority in next pollingInterval
	 */
	private void pushFailedMonitoringDevicesIDToPriority() {
		if (!failedMonitoringDeviceIds.isEmpty()) {
			deviceIds = deviceIds.stream().filter(id -> !failedMonitoringDeviceIds.contains(id)).collect(Collectors.toSet());
			deviceIds.addAll(failedMonitoringDeviceIds);
			failedMonitoringDeviceIds.clear();
		}
	}

	/**
	 * This method is used to format order number base on size of objects
	 *
	 * @param index index number
	 * @param objects list of object
	 * @return String location
	 */
	private String formatOrderNumber(int index, List<Object> objects) {
		Integer digitsQuantityOfRoomsQuantity = String.valueOf(objects.size() - 1).length();
		String roomIdFormat = "%0".concat(digitsQuantityOfRoomsQuantity.toString()).concat("d");
		return String.format(roomIdFormat, index);
	}


	/**
	 * This method is used to validate input config management from user
	 *
	 * @return boolean is configManagement
	 */
	public void isValidConfigManagement() {
		isConfigManagement = StringUtils.isNotNullOrEmpty(this.configManagement) && this.configManagement.equalsIgnoreCase("true");
	}
}