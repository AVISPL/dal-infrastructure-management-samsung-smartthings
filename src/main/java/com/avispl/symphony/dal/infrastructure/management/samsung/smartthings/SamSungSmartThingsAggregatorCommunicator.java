package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings;

import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.HubInfoMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsConstant;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsURL;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.location.LocationManagementMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.room.CreateRoomMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.room.RoomManagementMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.Hub;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.Device;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceCapability;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceCapabilityWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceHealth;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location.Location;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location.LocationWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DevicePresentation;
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


		public SamSungSmartThingsDeviceDataLoader() {
			inProgress = true;
		}

		@Override
		public void run() {
			mainloop:
			while (inProgress) {
				try {
					TimeUnit.MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					// Ignore for now
				}
				if (!inProgress) {
					break mainloop;
				}

				// next line will determine whether SamSungSmartThings monitoring was paused
				updateAggregatorStatus();
				if (devicePaused) {
					continue mainloop;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Fetching SmartThings devices list");
				}
				long currentTimestamp = System.currentTimeMillis();
				retrieveInfo(currentTimestamp);

				Set<String> presentationIds = cachedDevices.values().stream().map(Device::getPresentationId).collect(Collectors.toSet());

				if (logger.isDebugEnabled()) {
					logger.debug("Fetching other than SmartThings device list");
				}
				if (!cachedDevices.isEmpty() && validDeviceMetaDataRetrievalPeriodTimestamp <= currentTimestamp) {
					validDeviceMetaDataRetrievalPeriodTimestamp = currentTimestamp + deviceMetaDataRetrievalTimeout;

					for (Device device : cachedDevices.values()) {
						devicesExecutionPool.add(executorService.submit(() -> {
							try {
								if (logger.isDebugEnabled()) {
									logger.debug("Start one thread in worker thread: " + LocalDateTime.now());
								}
								retrieveDeviceHealth(device.getDeviceId());
								retrieveDevicePresentation(device);

								if (presentationIds.contains(device.getPresentationId())) {
									retrieveDevicePresentation(device);
									presentationIds.remove(device.getPresentationId());
								}
								mapDevicesToAggregatedDevice(cachedDevices.get(device.getDeviceId()));
								if (logger.isDebugEnabled()) {
									logger.debug("Finished one thread in worker thread: " + LocalDateTime.now());
								}
							} catch (Exception e) {
								logger.error(String.format("Exception during retrieve '%s' data processing.", device.getName()), e);
							}
						}));
					}
					do {
						try {
							TimeUnit.MILLISECONDS.sleep(500);
						} catch (InterruptedException e) {
							if (!inProgress) {
								break;
							}
						}
						devicesExecutionPool.removeIf(Future::isDone);
					} while (!devicesExecutionPool.isEmpty());
				}
				if (!inProgress) {
					break mainloop;
				}
				int aggregatedDevicesCount = aggregatedDevices.size();
				if (aggregatedDevicesCount == 0) {
					continue mainloop;
				}

				nextDevicesCollectionIterationTimestamp = System.currentTimeMillis();
				while (nextDevicesCollectionIterationTimestamp > System.currentTimeMillis()) {
					try {
						TimeUnit.MILLISECONDS.sleep(1000);
					} catch (InterruptedException e) {
						//
					}
				}

				if (!aggregatedDevices.isEmpty()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Applying filter options");
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Aggregated devices after applying filter: " + aggregatedDevices);
					}
				}

				// We don't want to fetch devices statuses too often, so by default it's currentTime + 30s
				// otherwise - the variable is reset by the retrieveMultipleStatistics() call, which
				// launches devices detailed statistics collection
				nextDevicesCollectionIterationTimestamp = System.currentTimeMillis() + 30000;

				if (logger.isDebugEnabled()) {
					logger.debug("Finished collecting devices statistics cycle at " + new Date());
				}
			}
			// Finished collecting
		}

		/**
		 * Triggers main loop to stop
		 */
		public void stop() {
			inProgress = false;
		}
	}

	/**
	 * This parameter holds timestamp of when we need to stop performing API calls
	 * It used when device stop retrieving statistic. Updated each time of called #retrieveMultipleStatistics
	 */
	private volatile long validRetrieveStatisticsTimestamp;

	/**
	 * Update the status of the device.
	 * The device is considered as paused if did not receive any retrieveMultipleStatistics()
	 * calls during {@link SamSungSmartThingsAggregatorCommunicator#validRetrieveStatisticsTimestamp}
	 */
	private synchronized void updateAggregatorStatus() {
		devicePaused = validRetrieveStatisticsTimestamp < System.currentTimeMillis();
	}

	/**
	 * Indicates whether a device is considered as paused.
	 * True by default so if the system is rebooted and the actual value is lost -> the device won't start stats
	 * collection unless the {@link SamSungSmartThingsAggregatorCommunicator#retrieveMultipleStatistics()} method is called which will change it
	 * to a correct value
	 */
	private volatile boolean devicePaused = true;

	/**
	 * Aggregator inactivity timeout. If the {@link SamSungSmartThingsAggregatorCommunicator#retrieveMultipleStatistics()}  method is not
	 * called during this period of time - device is considered to be paused, thus the Cloud API
	 * is not supposed to be called
	 */
	private static final long RETRIEVE_STATISTICS_TIME_OUT = 3 * 60 * 1000;

	/**
	 * Device metadata retrieval timeout. The general devices list is retrieved once during this time period.
	 */
	private long deviceMetaDataRetrievalTimeout = 60 * 1000 / 2;

	/**
	 * Time period within which the device metadata (basic devices information) cannot be refreshed.
	 * Ignored if device list is not yet retrieved or the cached device list is empty {@link SamSungSmartThingsAggregatorCommunicator#aggregatedDevices}
	 */
	private volatile long validDeviceMetaDataRetrievalPeriodTimestamp;

	/**
	 * We don't want the statistics to be collected constantly, because if there's not a big list of devices -
	 * new devices' statistics loop will be launched before the next monitoring iteration. To avoid that -
	 * this variable stores a timestamp which validates it, so when the devices' statistics is done collecting, variable
	 * is set to currentTime + 30s, at the same time, calling {@link #retrieveMultipleStatistics()} and updating the
	 * {@link #aggregatedDevices} resets it to the currentTime timestamp, which will re-activate data collection.
	 */
	private long nextDevicesCollectionIterationTimestamp;

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
	private volatile Room cachedCreateRoom = new Room();

	/**
	 * Caching the list of scenes data
	 */
	private List<Scene> cachedScenes = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Caching the list of devices data
	 */
	private ConcurrentHashMap<String, Device> cachedDevices = new ConcurrentHashMap<>();

	/**
	 * Caching the list of devices capabilities data
	 */
	private List<DeviceCapability> cachedCapabilities = Collections.synchronizedList(new ArrayList<>());

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
	 * List error message occur while fetching aggregated devices
	 */
	private Set<String> deviceErrorMessagesList = Collections.synchronizedSet(new LinkedHashSet<>());

	/**
	 * ReentrantLock to prevent null pointer exception to localExtendedStatistics when controlProperty method is called before GetMultipleStatistics method.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	// Adapter properties
	private String locationFilter;

	private String locationIdFiltered;
	private ExtendedStatistics localExtendedStatistics;
	private boolean isEmergencyDelivery = false;
	private Boolean isEditedForCreateRoom = false;
	ObjectMapper objectMapper = new ObjectMapper();

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
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called.");
		}
		apiToken = this.getPassword();
		this.setBaseUri(SmartThingsURL.BASE_URI);
		if (validateApiToken()) {
			executorService = Executors.newFixedThreadPool(8);
			executorService.submit(deviceDataLoader = new SamSungSmartThingsDeviceDataLoader());
			validDeviceMetaDataRetrievalPeriodTimestamp = System.currentTimeMillis();
		}
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

			if (!cachedDevices.isEmpty() && !isEmergencyDelivery) {
				populateLocationsManagement(stats, advancedControllableProperties);
				populateCreateRoomManagement(stats, advancedControllableProperties);
				retrieveRooms(stats, advancedControllableProperties);
				retrieveScenes(stats, advancedControllableProperties);

				String hubId = findDeviceIdByCategory(DeviceCategoriesMetric.HUB.getName());
				if (!hubId.isEmpty()) {
					retrieveHubDetailInfo(stats, hubId);
					retrieveHubHealth(stats, hubId);
				}
				extendedStatistics.setStatistics(stats);
				extendedStatistics.setControllableProperties(advancedControllableProperties);
				localExtendedStatistics = extendedStatistics;
			}
			isEmergencyDelivery = false;
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
		if (validateApiToken()) {
			if (executorService == null) {
				// Due to the bug that after changing properties on fly - the adapter is destroyed but adapter is not initialized properly,
				// so executor service is not running. We need to make sure executorService exists
				executorService = Executors.newFixedThreadPool(8);
				executorService.submit(deviceDataLoader = new SamSungSmartThingsDeviceDataLoader());
			}
			nextDevicesCollectionIterationTimestamp = System.currentTimeMillis();
			updateValidRetrieveStatisticsTimestamp();
		}
		if (!deviceErrorMessagesList.isEmpty()) {
			synchronized (deviceErrorMessagesList) {
				String errorMessage = deviceErrorMessagesList.stream().map(Object::toString)
						.collect(Collectors.joining("\n"));
				deviceErrorMessagesList.clear();
				throw new ResourceNotReachableException(errorMessage);
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
				} catch (SocketTimeoutException tex) {
					if (this.logger.isDebugEnabled()) {
						this.logger.debug(String.format("PING TIMEOUT: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
					}
					return this.getPingTimeout();
				}
			}
			return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
		} else {
			throw new IllegalStateException("Cannot use device class without calling init() first");
		}
	}

	/**
	 * Uptime time stamp to valid one
	 */
	private synchronized void updateValidRetrieveStatisticsTimestamp() {
		validRetrieveStatisticsTimestamp = System.currentTimeMillis() + RETRIEVE_STATISTICS_TIME_OUT;
		updateAggregatorStatus();
	}

	//region retrieve aggregator info: hub inventory, hub heath, locations, rooms, scene
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used to retrieve hub detail info by send GET request to "https://api.smartthings.com/v1/hubdevices/{hubId}"
	 *
	 * @param stats store all statistics
	 * @param hubId id of SmartThings hub
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveHubDetailInfo(Map<String, String> stats, String hubId) {
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
			throw new ResourceNotReachableException("Error while retrieve hub info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to retrieve hub health info by send GET request to "https://api.smartthings.com/v1/hubdevices/{hubId}/health"
	 *
	 * @param stats store all statistics
	 * @param hubId id of SmartThings hub
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveHubHealth(Map<String, String> stats, String hubId) {
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
			throw new ResourceNotReachableException("Error while retrieve hub info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to retrieve rooms info by send GET request to "https://api.smartthings.com/v1/locations/{locationId}/rooms"
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveRooms(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
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
			throw new ResourceNotReachableException("Error while retrieve rooms info: " + e.getMessage(), e);
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
			stats.put(AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.EDITED, isEditedForCreateRoom.toString());
			addAdvanceControlProperties(advancedControllableProperties,
					createButton(stats, AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.CANCEL.getName(), SmartThingsConstant.CANCEL, SmartThingsConstant.CANCELING));
		}
	}

	/**
	 * This method is used to retrieve scenes info by send GET request to "https://api.smartthings.com/v1/scene?locationId={locationId}"
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveScenes(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
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
			throw new ResourceNotReachableException("Error while retrieve rooms info: " + e.getMessage(), e);
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

	//region retrieve aggregated device info: locations, aggregated device and device capabilities info in worker thread
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Retrieve aggregated devices and system information data -
	 * and set next device/system collection iteration timestamp
	 *
	 * @param currentTimestamp current time stamp
	 */
	private void retrieveInfo(long currentTimestamp) {
		if (validDeviceMetaDataRetrievalPeriodTimestamp > currentTimestamp) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Aggregated devices data retrieval is in cool down. %s seconds left",
						(validDeviceMetaDataRetrievalPeriodTimestamp - currentTimestamp) / 1000));
				if (!aggregatedDevices.isEmpty()) {
					logger.debug(String.format("Old fetched devices list: %s", aggregatedDevices));
				}
				if (!cachedCapabilities.isEmpty()) {
					logger.debug(String.format("Old fetched capabilities list: %s", cachedCapabilities));
				}
				if (!cachedLocations.isEmpty()) {
					logger.debug(String.format("Old fetched capabilities list: %s", cachedCapabilities));
				}
			}
			return;
		}
		retrieveLocations();
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("New fetched devices list: %s", cachedLocations));
		}
		retrieveDevices();
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("New fetched devices list: %s", aggregatedDevices));
		}
		retrieveCapabilities();
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("New fetched capabilities list: %s", cachedCapabilities));
		}
	}

	/**
	 * This method is used to retrieve list of device every 30 seconds by send GET request to https://api.smartthings.com/v1/devices?locationId={locationId}
	 *
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	private void retrieveDevices() {
		try {
			String request = SmartThingsURL.DEVICES
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.LOCATION_ID)
					.concat(locationIdFiltered);

			DeviceWrapper responseDeviceList = this.doGet(request, DeviceWrapper.class);
			cachedDevices = (ConcurrentHashMap<String, Device>) responseDeviceList.getDevices().stream().collect(Collectors.toConcurrentMap(Device::getDeviceId, Function.identity()));

		} catch (Exception e) {
			String errorMessage = String.format("Aggregated Device Data Retrieval-Error: %s with cause: %s", e.getMessage(), e.getCause().getMessage());
			deviceErrorMessagesList.add(errorMessage);
			logger.error(errorMessage, e);
		}
	}

	/**
	 * This method is used to retrieve list of device capabilities every 30 seconds by send GET request to https://api.smartthings.com/v1/capabilities
	 *
	 * @throws ResourceNotReachableException If any error occurs
	 */
	private void retrieveCapabilities() {
		try {
			String request = SmartThingsURL.CAPABILITIES;

			DeviceCapabilityWrapper deviceCapabilitiesWrapper = this.doGet(request, DeviceCapabilityWrapper.class);
			cachedCapabilities = deviceCapabilitiesWrapper.getDeviceCapabilities();

		} catch (Exception e) {
			String errorMessage = String.format("Device capabilities Data Retrieval-Error: %s with cause: %s", e.getMessage(), e.getCause().getMessage());
			throw new ResourceNotReachableException(errorMessage, e);
		}
	}

	/**
	 * This method is used to retrieve locations info by send GET request to "https://api.smartthings.com/v1/locations"
	 *
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveLocations() {
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
			throw new ResourceNotReachableException("Error while retrieve locations info: " + e.getMessage(), e);
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

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region retrieve detail aggregated device info: device health, device presentation and device full status in worker thread
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used to retrieve list of device health by send GET request to https://api.smartthings.com/v1/devices/{deviceId}/health
	 *
	 * @param deviceID id of device
	 *
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
			String errorMessage = String.format("Error while retrieve %s health info: %s ", cachedDevices.get(deviceID).getName(), e.getMessage());
			deviceErrorMessagesList.add(errorMessage);
			logger.error(errorMessage, e);
		}
	}

	/**
	 * This method is used to retrieve list of device health by send GET request to
	 * https://api.smartthings.com/v1/presentation?presentationId={presentationId}&manufacturerName={manufacturerName}
	 *
	 * @param device device info
	 *
	 * @throws ResourceNotReachableException If any error occurs
	 */
	private void retrieveDevicePresentation(Device device) {
		try {
			String request = SmartThingsURL.PRESENTATION
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.PRESENTATION_ID)
					.concat(device.getPresentationId())
					.concat(SmartThingsConstant.AMPERSAND)
					.concat(SmartThingsURL.MANUFACTURE_NAME)
					.concat(device.getManufacturerName());

			DevicePresentation devicePresentation = doGet(request, DevicePresentation.class);

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
			String errorMessage = String.format("Error while retrieve %s presentation info: %s", device.getName(), e.getMessage());
			deviceErrorMessagesList.add(errorMessage);
			logger.error(errorMessage, e);
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
	 *
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
	 *
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
	 *
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
	 *
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

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	/**
	 * Handle rate limit exceed error while controlling
	 *
	 * @param response ResponseEntity
	 *
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
}
