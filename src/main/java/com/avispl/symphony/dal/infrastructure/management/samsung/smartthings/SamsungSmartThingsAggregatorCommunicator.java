package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings;

import java.awt.Color;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
import com.google.common.base.Throwables;
import com.google.common.math.IntMath;
import javax.security.auth.login.FailedLoginException;

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
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.AggregatedDeviceColorControllingConstant;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.AggregatedDeviceControllingMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.AggregatedDeviceDropdownListModesControllingConstant;
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
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.ColorDevicePresentation;
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
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.Alternative;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.Command;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.DropdownList;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.Slider;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.State;
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
	 * Map of aggregated devices
	 */
	private ConcurrentHashMap<String, AggregatedDevice> cachedAggregatedDevices = new ConcurrentHashMap<>();

	/**
	 * Map of controllablePropertiesName after converted to TitleCase
	 */
	private Map<String, String> controllablePropertyNames = new HashMap<>();

	/**
	 * Executor that runs all the async operations
	 */
	private static ExecutorService executorService;

	/**
	 * ReentrantLock to prevent null pointer exception to localExtendedStatistics when controlProperty method is called before GetMultipleStatistics method.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	/**
	 * store locationFilter adapter properties
	 */
	private String locationFilter;

	/**
	 * store pollingInterval adapter properties
	 */
	private String pollingInterval;

	/**
	 * store deviceTypesFilter adapter properties
	 */
	private String deviceTypeFilter;

	/**
	 * store deviceNamesFilter adapter properties
	 */
	private String deviceNameFilter;

	/**
	 * store roomsFilter adapter properties
	 */
	private String roomFilter;

	/**
	 * store configManagement adapter properties
	 */
	private String configManagement;

	/**
	 * configManagement in boolean value
	 */
	private boolean isConfigManagement;

	/**
	 * set of unused Devices controllable properties keys
	 */
	private Set<String> unusedDeviceControlKeys = new HashSet<>();

	/**
	 * set of unused EditRoom controllable properties keys
	 */
	private Set<String> unusedRoomControlKeys = new HashSet<>();

	/**
	 * location ID after filtering
	 */
	private String locationIdFiltered;

	/**
	 * store next polling interval
	 */
	private long nextPollingInterval;

	private ExtendedStatistics localExtendedStatistics;
	private boolean isEmergencyDelivery = false;
	private Boolean isEditedForCreateRoom = false;
	private ObjectMapper objectMapper = new ObjectMapper();


	/**
	 * Stored common color
	 */
	private static Map<String, Color> commonColors = new HashMap<>();

	/**
	 * Stored media playback modes
	 */
	private static Map<String, String> mediaPlaybackModes = new HashMap<>();

	/**
	 * Polling interval which applied in adapter
	 */
	private volatile int localPollingInterval = SmartThingsConstant.MIN_POLLING_INTERVAL;

	/**
	 * The current phase of monitoring cycle in polling interval
	 */
	private final AtomicInteger currentPhase = new AtomicInteger(0);

	//region retrieve detail aggregated device info: device health, device presentation and device full status in worker thread
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Submit thread to get device detail info
	 *
	 * @param currentThread current thread index
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
				logger.error(String.format("%s health info is empty, Please go to https://graph-ap02-apnortheast2.api.smartthings.com/ to check if the device are removed from Smartthings",
						cachedDevicesAfterPollingInterval.get(deviceID).getName()));
			}
		} catch (Exception e) {
			failedMonitoringDeviceIds.add(deviceID);
			logger.error(String.format("Error while retrieve %s health info: %s ", cachedDevicesAfterPollingInterval.get(deviceID).getName(), e.getMessage()), e);
		}
	}

	/**
	 * This method is used to retrieve list of device health by send GET request to
	 * https://api.smartthings.com/v1/presentation?presentationId={presentationId}&manufacturerName={manufacturerName}
	 *
	 * @param deviceId device ID
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
				logger.error(
						String.format("%s presentation info is empty, Please go to https://graph-ap02-apnortheast2.api.smartthings.com/ to check if the device are removed from Smartthings", device.getName()));
			}
		} catch (Exception e) {
			failedMonitoringDeviceIds.add(deviceId);
			logger.error(String.format("Error while retrieve %s presentation info: %s", device.getName(), e.getMessage()), e);
		}
	}

	/**
	 * This method is used to retrieve device full status by send GET request to
	 * https://api.smartthings.com/v1/devices/{deviceId}/status
	 *
	 * @param deviceId device ID
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

				List<DetailViewPresentation> detailViewPresentationsAfterMapping = mapControllablePropertyStatusToDevice(response, detailViewPresentations.get(),
						cachedDevices.get(deviceId).getPresentation());
				cachedDevices.get(deviceId).getPresentation().setDetailViewPresentations(detailViewPresentationsAfterMapping);

				if (dashBoardActions.isPresent()) {
					List<DetailViewPresentation> dashboardActionsAfterMapping = mapControllablePropertyStatusToDevice(response, dashBoardActions.get(), cachedDevices.get(deviceId).getPresentation());
					cachedDevices.get(deviceId).getPresentation().getDashboardPresentations().setActions(dashboardActionsAfterMapping);
				}
			} else {
				logger.error(
						String.format("%s full controllable properties status info is empty, Please go to https://graph-ap02-apnortheast2.api.smartthings.com/ to check if the device are removed from Smartthings",
								cachedDevicesAfterPollingInterval.get(deviceId).getName()));
			}
		} catch (Exception e) {
			failedMonitoringDeviceIds.add(deviceId);
			logger.error(String.format("Error while retrieve %s full controllable properties status info: %s", cachedDevicesAfterPollingInterval.get(deviceId).getName(), e.getMessage()), e);
		}
	}

	/**
	 * This method is used to map controllable property status to device
	 *
	 * @param response Json response
	 * @param detailViewPresentations list of detail view presentation of device
	 */
	private List<DetailViewPresentation> mapControllablePropertyStatusToDevice(ObjectNode response, List<DetailViewPresentation> detailViewPresentations, DevicePresentation devicePresentation) {

		for (DetailViewPresentation detailViewPresentation : detailViewPresentations) {

			Optional<Iterator<JsonNode>> controllablePropertiesStatus = Optional.ofNullable(response.elements().next())
					.map(JsonNode::elements)
					.map(Iterator::next)
					.map(c -> c.get(detailViewPresentation.getCapability()))
					.map(JsonNode::elements);

			String value = SmartThingsConstant.NONE;
			String unit = SmartThingsConstant.NONE;

			// map specific controllable property status to device
			switch (detailViewPresentation.getCapability()) {
				case AggregatedDeviceColorControllingConstant.COLOR_CONTROL:
					if (detailViewPresentation.getDisplayType().equals(DeviceDisplayTypesMetric.NUMBER_FIELD)) {
						if (AggregatedDeviceColorControllingConstant.COLOR_CONTROL_SET_HUE.equals(detailViewPresentation.getNumberField().getCommand())) {
							value = Optional.ofNullable(response.elements().next())
									.map(JsonNode::elements)
									.map(Iterator::next)
									.map(c -> c.get(detailViewPresentation.getCapability()))
									.map(d -> d.get(AggregatedDeviceColorControllingConstant.COLOR_CONTROL_HUE))
									.map(u -> u.get(SmartThingsConstant.VALUE))
									.map(JsonNode::asText)
									.orElse(SmartThingsConstant.NONE);
							devicePresentation.getColor().setHue(Float.parseFloat(value));
							continue;
						}
						if (AggregatedDeviceColorControllingConstant.COLOR_CONTROL_SET_SATURATION.equals(detailViewPresentation.getNumberField().getCommand())) {
							value = Optional.ofNullable(response.elements().next())
									.map(JsonNode::elements)
									.map(Iterator::next)
									.map(c -> c.get(detailViewPresentation.getCapability()))
									.map(d -> d.get(AggregatedDeviceColorControllingConstant.COLOR_CONTROL_SATURATION))
									.map(u -> u.get(SmartThingsConstant.VALUE))
									.map(JsonNode::asText)
									.orElse(SmartThingsConstant.NONE);
							devicePresentation.getColor().setSaturation(Float.parseFloat(value));
							continue;
						}
					}
					break;
				default:
					// we do not log error/ warning here, we are going to map data for common controllable properties in the next step
					break;
			}

			// validate and assign default value for null controllable properties status
			boolean isHavingValueData = false;
			if (controllablePropertiesStatus.isPresent()) {
				isHavingValueData = controllablePropertiesStatus.get().hasNext();
			}
			if (isHavingValueData) {
				value = Optional.ofNullable(controllablePropertiesStatus.get().next())
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

			// map common controllable property status to device
			switch (detailViewPresentation.getDisplayType()) {
				case DeviceDisplayTypesMetric.SLIDER:
					if (isHavingValueData) {
						unit = Optional.ofNullable(response.elements().next())
								.map(JsonNode::elements)
								.map(Iterator::next)
								.map(c -> c.get(detailViewPresentation.getCapability()))
								.map(JsonNode::elements)
								.map(Iterator::next)
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
					if (AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_MODE.equals(detailViewPresentation.getCapability())) {
						String state = Optional.ofNullable(response.elements().next())
								.map(JsonNode::elements)
								.map(Iterator::next)
								.map(c -> c.get(detailViewPresentation.getCapability()))
								.map(d -> d.get(SmartThingsConstant.TV_PLAYBACK_STATUS))
								.map(u -> u.get(SmartThingsConstant.VALUE))
								.map(JsonNode::asText)
								.orElse(SmartThingsConstant.NONE);
						detailViewPresentation.getDropdownList().getState().setValue(state);
					}
					break;
				case DeviceDisplayTypesMetric.NUMBER_FIELD:
					detailViewPresentation.getNumberField().setValue(value);
					break;
				case DeviceDisplayTypesMetric.TEXT_FIELD:
					detailViewPresentation.getTextField().setValue(value);
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
	 * Retrieves {@link #locationFilter}
	 *
	 * @return value of {@link #locationFilter}
	 */
	public String getLocationFilter() {
		return locationFilter;
	}

	/**
	 * Sets {@link #locationFilter} value
	 *
	 * @param locationFilter new value of {@link #locationFilter}
	 */
	public void setLocationFilter(String locationFilter) {
		this.locationFilter = locationFilter;
	}

	/**
	 * Retrieves {@link #pollingInterval}
	 *
	 * @return value of {@link #pollingInterval}
	 */
	public String getPollingInterval() {
		return pollingInterval;
	}

	/**
	 * Sets {@link #pollingInterval} value
	 *
	 * @param pollingInterval new value of {@link #pollingInterval}
	 */
	public void setPollingInterval(String pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	/**
	 * Retrieves {@link #deviceTypeFilter}
	 *
	 * @return value of {@link #deviceTypeFilter}
	 */
	public String getDeviceTypeFilter() {
		return deviceTypeFilter;
	}

	/**
	 * Sets {@link #deviceTypeFilter} value
	 *
	 * @param deviceTypeFilter new value of {@link #deviceTypeFilter}
	 */
	public void setDeviceTypeFilter(String deviceTypeFilter) {
		this.deviceTypeFilter = deviceTypeFilter;
	}

	/**
	 * Retrieves {@link #deviceNameFilter}
	 *
	 * @return value of {@link #deviceNameFilter}
	 */
	public String getDeviceNameFilter() {
		return deviceNameFilter;
	}

	/**
	 * Sets {@link #deviceNameFilter} value
	 *
	 * @param deviceNameFilter new value of {@link #deviceNameFilter}
	 */
	public void setDeviceNameFilter(String deviceNameFilter) {
		this.deviceNameFilter = deviceNameFilter;
	}

	/**
	 * Retrieves {@link #roomFilter}
	 *
	 * @return value of {@link #roomFilter}
	 */
	public String getRoomFilter() {
		return roomFilter;
	}

	/**
	 * Sets {@link #roomFilter} value
	 *
	 * @param roomFilter new value of {@link #roomFilter}
	 */
	public void setRoomFilter(String roomFilter) {
		this.roomFilter = roomFilter;
	}

	/**
	 * Retrieves {@link #configManagement}
	 *
	 * @return value of {@link #configManagement}
	 */
	public String getConfigManagement() {
		return configManagement;
	}

	/**
	 * Sets {@link #configManagement} value
	 *
	 * @param configManagement new value of {@link #configManagement}
	 */
	public void setConfigManagement(String configManagement) {
		this.configManagement = configManagement;
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
		initCommonColors();
		initMediaPlayBackList();
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
			if (!isEmergencyDelivery) {
				ExtendedStatistics extendedStatistics = new ExtendedStatistics();
				Map<String, String> stats = new HashMap<>();
				List<AdvancedControllableProperty> advancedControllableProperties = new ArrayList<>();
				isValidConfigManagement();

				if (currentPhase.get() == localPollingInterval || currentPhase.get() == SmartThingsConstant.FIRST_MONITORING_CYCLE_OF_POLLING_INTERVAL) {
					// retrieve device and filter devices in first monitoring cycle of polling interval
					cachedDevicesAfterPollingInterval = (ConcurrentHashMap<String, Device>) cachedDevices.values().stream().collect(Collectors.toConcurrentMap(Device::getDeviceId, Device::new));
					mapAggregatedDevicesToCache();
					retrieveInfo();
					filterDeviceIds();

					// calculating polling interval and threads quantity
					localPollingInterval = calculatingLocalPollingInterval();
					deviceStatisticsCollectionThreads = calculatingThreadQuantity();
					pushFailedMonitoringDevicesIDToPriority();
					nextPollingInterval = System.currentTimeMillis() + localPollingInterval * 1000;
					currentPhase.set(0);
				}
				populateCurrentLocation(stats);

				// populate edit location, create room, edit room group when configManagement is true
				if (isConfigManagement) {
					populateLocationsManagement(stats, advancedControllableProperties);
					if (!SmartThingsConstant.NO_LOCATION_FOUND.equals(locationIdFiltered)) {
						populateRoomManagement(stats, advancedControllableProperties);
						populateCreateRoomManagement(stats, advancedControllableProperties);
					}
				}
				if (!SmartThingsConstant.NO_LOCATION_FOUND.equals(locationIdFiltered)) {
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

						// Submit thread to get aggregated device
						currentPhase.incrementAndGet();
						if (executorService == null) {
							executorService = Executors.newFixedThreadPool(deviceStatisticsCollectionThreads);
						}
						for (int threadNumber = 0; threadNumber < deviceStatisticsCollectionThreads; threadNumber++) {
							executorService.submit(new SamsungSmartThingsDeviceDataLoader(threadNumber));
						}
					}
					populateDeviceDashboardView(stats, advancedControllableProperties);
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
		String deviceId = controllableProperty.getDeviceId();
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
			if (splitProperty.length == 2) {

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
			} else if (cachedAggregatedDevices.get(deviceId) != null) {
				AggregatedDeviceControllingMetric aggregatedDeviceControllingMetric = AggregatedDeviceControllingMetric.getByName(property);
				AggregatedDevice aggregatedDevice = cachedAggregatedDevices.get(deviceId);
				if (!aggregatedDevice.getDeviceOnline()) {
					throw new IllegalStateException(String.format("Unable to control %s, device is offline", aggregatedDevice.getDeviceName()));
				}
				if (aggregatedDevice.getProperties() == null) {
					throw new ResourceNotReachableException("The device's properties are null, please wait until the next polling interval");
				}
				Map<String, String> aggregatedDeviceProperties = cachedAggregatedDevices.get(deviceId).getProperties();
				List<AdvancedControllableProperty> aggregatedDeviceControllableProperties = cachedAggregatedDevices.get(deviceId).getControllableProperties();
				switch (aggregatedDeviceControllingMetric) {
					case ROOM_MANAGEMENT:
						aggregatedDeviceRoomControl(aggregatedDeviceProperties, aggregatedDeviceControllableProperties, property, value, deviceId);
						break;
					case COLOR_CONTROL:
						aggregatedDeviceColorDropdownControl(aggregatedDeviceProperties, aggregatedDeviceControllableProperties, property, value, deviceId);
						break;
					case HUE_CONTROL:
						Float hue = convertHueToSmartThingsValue(Float.parseFloat(value));
						aggregatedDeviceColorHueControl(aggregatedDeviceProperties, aggregatedDeviceControllableProperties, property, hue, deviceId);
						break;
					case SATURATION_CONTROL:
						Float saturation = Float.parseFloat(value);
						aggregatedDeviceColorSaturationControl(aggregatedDeviceProperties, aggregatedDeviceControllableProperties, property, saturation, deviceId);
						break;
					case AGGREGATED_DEVICE:
						property = controllablePropertyNames.get(property);
						aggregatedDeviceControl(aggregatedDeviceProperties, aggregatedDeviceControllableProperties, property, value, deviceId);
						break;
					default:
						if (logger.isWarnEnabled()) {
							logger.warn(String.format("Controlling group %s is not supported.", aggregatedDeviceControllingMetric.getName()));
						}
						throw new IllegalStateException(String.format("Controlling group %s is not supported.", aggregatedDeviceControllingMetric.getName()));
				}
				populateDeviceDashboardView(stats, advancedControllableProperties);
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
		return cachedAggregatedDevices.values().stream().collect(Collectors.toList());
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
		if (executorService != null) {
			executorService.shutdownNow();
			executorService = null;
		}
		localPollingInterval = SmartThingsConstant.FIRST_MONITORING_CYCLE_OF_POLLING_INTERVAL;
		cachedRooms.clear();
		cachedDevices.clear();
		cachedDevicesAfterPollingInterval.clear();
		cachedAggregatedDevices.clear();
		cachedPresentations.clear();
		cachedLocations.clear();
		controllablePropertyNames.clear();
		cachedScenes.clear();
		if (localExtendedStatistics.getStatistics() != null) {
			localExtendedStatistics.getStatistics().clear();
		}
		if (localExtendedStatistics.getControllableProperties() != null) {
			localExtendedStatistics.getControllableProperties().clear();
		}
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
	 */
	private void retrieveHubDetailInfo(Map<String, String> stats, String hubId, boolean retryOnError) {
		try {
			String request = SmartThingsURL.HUB_DEVICE.concat(hubId);
			Hub hub = doGet(request, Hub.class);

			if (hub != null) {
				stats.put(HubInfoMetric.NAME.getName(), hub.getName());
				stats.put(HubInfoMetric.FIRMWARE_VERSION.getName(), getDefaultValueForNullData(hub.getFirmwareVersion(), SmartThingsConstant.EMPTY));
			} else {
				logger.error("Hub information is empty, Please go to https://graph-ap02-apnortheast2.api.smartthings.com/ to check if the hub are removed from Smartthings");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveHubDetailInfo(stats, hubId, false);
			}
			logger.error("Error while retrieve hub info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to retrieve hub health info by send GET request to "https://api.smartthings.com/v1/hubdevices/{hubId}/health"
	 *
	 * @param stats store all statistics
	 * @param hubId id of SmartThings hub
	 * @param retryOnError retry if any error occurs
	 */
	private void retrieveHubHealth(Map<String, String> stats, String hubId, boolean retryOnError) {
		try {
			String request = SmartThingsURL.DEVICES
					.concat(SmartThingsConstant.SLASH)
					.concat(hubId)
					.concat(SmartThingsURL.DEVICE_HEALTH);

			Hub hub = doGet(request, Hub.class);

			if (hub != null) {
				stats.put(HubInfoMetric.STATE.getName(), getDefaultValueForNullData(hub.getState(), SmartThingsConstant.NONE));
			} else {
				logger.error("Hub health info is empty, Please go to https://graph-ap02-apnortheast2.api.smartthings.com/ to check if the hub are removed from Smartthings");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveHubHealth(stats, hubId, false);
			}
			logger.error("Error while retrieve hub info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to retrieve locations info by send GET request to "https://api.smartthings.com/v1/locations"
	 *
	 * @param retryOnError retry if any error occurs
	 */
	private void retrieveLocations(boolean retryOnError) {
		try {
			String request = SmartThingsURL.LOCATIONS;

			LocationWrapper locationWrapper = doGet(request, LocationWrapper.class);

			if (locationWrapper != null) {
				cachedLocations = locationWrapper.getLocations();
				locationIdFiltered = getDefaultValueForNullData(findLocationByName(locationFilter).getLocationId(), SmartThingsConstant.NO_LOCATION_FOUND);
			} else {
				logger.error("Locations is empty, Please go to https://graph-ap02-apnortheast2.api.smartthings.com/ to check if the locations are removed from Smartthings");
			}
		} catch (FailedLoginException failedLoginException) {
			throw new ResourceNotReachableException("Login failed, Please check the server address and the personal access token");
		} catch (Exception e) {
			if (Throwables.getRootCause(e) instanceof UnknownHostException) {
				throw new ResourceNotReachableException("Login failed, Please check the server address and the personal access token");
			}
			if (retryOnError) {
				retrieveLocations(false);
			}
			throw new ResourceNotReachableException("Error while retrieve locations info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to populate locations management group
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 */
	private void populateLocationsManagement(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		for (int locationIndex = 0; locationIndex < cachedLocations.size(); locationIndex++) {
			addAdvanceControlProperties(advancedControllableProperties, createText(stats, AggregatorGroupControllingMetric.LOCATION_MANAGEMENT.getName()
							+ LocationManagementMetric.LOCATION.getName() + formatOrderNumber(locationIndex, Arrays.asList(cachedLocations.toArray())),
					getDefaultValueForNullData(cachedLocations.get(locationIndex).getName(), SmartThingsConstant.NONE)));
		}
	}

	/**
	 * This method is used to populate current location
	 *
	 * @param stats store all statistics
	 */
	private void populateCurrentLocation(Map<String, String> stats) {
		String locationName = cachedLocations.stream().filter(l -> l.getLocationId().equals(locationIdFiltered)).map(Location::getName).findFirst()
				.orElse(SmartThingsConstant.NO_LOCATION_FOUND);
		stats.put(HubInfoMetric.CURRENT_LOCATION.getName(), locationName);
	}

	/**
	 * This method is used to retrieve rooms info by send GET request to "https://api.smartthings.com/v1/locations/{locationId}/rooms"
	 *
	 * @param retryOnError retry if any error occurs
	 */
	private void retrieveRooms(boolean retryOnError) {
		try {
			String request = SmartThingsURL.LOCATIONS
					.concat(SmartThingsConstant.SLASH)
					.concat(locationIdFiltered)
					.concat(SmartThingsConstant.SLASH)
					.concat(SmartThingsURL.ROOMS);

			RoomWrapper roomWrapper = doGet(request, RoomWrapper.class);

			if (roomWrapper != null && roomWrapper.getRooms() != null) {
				cachedRooms = roomWrapper.getRooms();
			} else {
				logger.error("rooms is empty, Please go to https://graph-ap02-apnortheast2.api.smartthings.com/ to check if the rooms are removed from Smartthings");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveRooms(false);
			}
			logger.error("Error while retrieve rooms info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to populate locations management group
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 */
	private void populateRoomManagement(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		boolean isEmptyRoomControl = true;

		if (unusedRoomControlKeys != null) {
			removeUnusedStatsAndControls(stats, advancedControllableProperties, unusedRoomControlKeys);
		}

		for (int roomIndex = 0; roomIndex < cachedRooms.size(); roomIndex++) {
			String editRoomKey = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + formatOrderNumber(roomIndex, Arrays.asList(cachedRooms.toArray()));
			String deleteRoomKey = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + formatOrderNumber(roomIndex, Arrays.asList(cachedRooms.toArray()))
					+ RoomManagementMetric.DELETE_ROOM.getName();

			addAdvanceControlProperties(advancedControllableProperties, createText(stats, editRoomKey, getDefaultValueForNullData(cachedRooms.get(roomIndex).getName(), SmartThingsConstant.NONE)));
			addAdvanceControlProperties(advancedControllableProperties, createButton(stats, deleteRoomKey, SmartThingsConstant.DELETE, SmartThingsConstant.DELETING));
			isEmptyRoomControl = false;

			unusedRoomControlKeys.add(editRoomKey);
			unusedRoomControlKeys.add(deleteRoomKey);
		}
		// populate message when location have no rooms after filtering
		if (isEmptyRoomControl) {
			stats.put(AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + AggregatorGroupControllingMetric.MESSAGE.getName(), "There are no rooms available in this location");
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
	private void populateCreateRoomManagement(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		String locationName = getDefaultValueForNullData(findLocationByName(locationFilter).getName(), SmartThingsConstant.NO_LOCATION_FOUND);
		stats.put(AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.LOCATION.getName(), getDefaultValueForNullData(locationName, SmartThingsConstant.EMPTY));
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
	 */
	private void retrieveScenes(boolean retryOnError) {
		try {
			String request = SmartThingsURL.SCENE
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.LOCATION_ID)
					.concat(locationIdFiltered);

			SceneWrapper sceneWrapper = doGet(request, SceneWrapper.class);

			if (sceneWrapper != null && sceneWrapper.getScenes() != null) {
				cachedScenes = sceneWrapper.getScenes();
			} else {
				logger.error("scenes is empty, Please go to mobile app to check if the scenes are removed from Smartthings");
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveScenes(false);
			}
			logger.error("Error while retrieve scene info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to populate scenes trigger group
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 */
	private void populateScenesManagement(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		boolean isEmptyScenesControl = true;
		for (Scene scene : cachedScenes) {
			if (StringUtils.isNotNullOrEmpty(scene.getSceneName())) {
				addAdvanceControlProperties(advancedControllableProperties,
						createButton(stats, AggregatorGroupControllingMetric.SCENE.getName().concat(scene.getSceneName()), SmartThingsConstant.RUN, SmartThingsConstant.RUNNING));
				isEmptyScenesControl = false;
			}
		}
		// populate message when location have no scene after filtering
		if (isEmptyScenesControl) {
			stats.put(AggregatorGroupControllingMetric.SCENE.getName() + AggregatorGroupControllingMetric.MESSAGE.getName(), "There are no scenes available in this location");
		} else {
			stats.remove(AggregatorGroupControllingMetric.SCENE.getName() + AggregatorGroupControllingMetric.MESSAGE.getName());
		}
	}

	/**
	 * This method is used to populate polling interval
	 *
	 * @param stats store all statistics
	 */
	private void populatePollingInterval(Map<String, String> stats) {
		Integer minPollingInterval = calculatingMinPollingInterval();

		Date date = new Date(nextPollingInterval);
		Format format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

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
	 */
	private void retrieveDevices(boolean retryOnError) {
		try {
			String request = SmartThingsURL.DEVICES
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.LOCATION_ID)
					.concat(locationIdFiltered);

			DeviceWrapper responseDeviceList = this.doGet(request, DeviceWrapper.class);

			if (responseDeviceList.getDevices() != null) {
				cachedDevices = (ConcurrentHashMap<String, Device>) responseDeviceList.getDevices().stream().collect(Collectors.toConcurrentMap(Device::getDeviceId, Function.identity()));
			}
		} catch (Exception e) {
			if (retryOnError) {
				retrieveDevices(false);
			}
			logger.error(String.format("Aggregated Device Data Retrieval-Error in locationID %s: %s with cause: %s", locationIdFiltered, e.getMessage(), e.getCause().getMessage()), e);
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
	 * @throws IllegalStateException when fail to control
	 */
	private void locationControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, String value) {
		int locationIndex = Integer.parseInt(controllableProperty.substring(LocationManagementMetric.LOCATION.getName().length()));
		try {
			for (int i = 0; i < cachedLocations.size(); i++) {
				if (i == locationIndex) {
					continue;
				}
				if (cachedLocations.get(i).getName().equalsIgnoreCase(value)) {
					throw new IllegalArgumentException(String.format("The location name %s already exists, Please chose the different location name", value));
				}
			}
			if (value.trim().isEmpty()) {
				throw new IllegalArgumentException("Invalid location name, the location name can not be empty");
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
				throw new IllegalStateException(String.format("Changing %s name fail, please try again later", controllableProperty));
			}

			addAdvanceControlProperties(advancedControllableProperties,
					createText(stats, AggregatorGroupControllingMetric.LOCATION_MANAGEMENT.getName() + LocationManagementMetric.LOCATION.getName() + locationIndex, value));
			isEmergencyDelivery = true;
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Error while controlling location %s %s: %s", controllableProperty, cachedLocations.get(locationIndex).getName(), e.getMessage()), e);
		}
	}

	/**
	 * This method is used for calling control room properties:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param value value of controllable property
	 * @throws IllegalStateException when fail to control
	 */
	private void roomControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, String value) {
		int roomIndex = Integer.parseInt(controllableProperty.substring(RoomManagementMetric.ROOM.getName().length(),
				RoomManagementMetric.ROOM.getName().length() + String.valueOf(cachedRooms.size() - SmartThingsConstant.CONVERT_POSITION_TO_INDEX).length()));
		try {
			for (int i = 0; i < cachedRooms.size(); i++) {
				if (i == roomIndex) {
					continue;
				}
				if (!controllableProperty.contains(SmartThingsConstant.DELETE) && cachedRooms.get(i).getName().equalsIgnoreCase(value)) {
					throw new IllegalArgumentException(String.format("The room name %s already exists, Please chose the different room name", value));
				}
			}
			if (value.trim().isEmpty()) {
				throw new IllegalArgumentException("Invalid room name, the room name can not be empty");
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
						throw new IllegalStateException(String.format("Changing %s name fail, please try again later", controllableProperty));
					}

					populateRoomManagement(stats, advancedControllableProperties);
					isEmergencyDelivery = true;
					break;
				case DELETE_ROOM:
					response = doRequest(request, HttpMethod.DELETE, headers, requestBody, String.class);

					handleRateLimitExceed(response);

					if (!response.getStatusCode().is2xxSuccessful()) {
						throw new IllegalStateException(String.format("%s fail, please try again later", controllableProperty));
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
			throw new IllegalStateException(String.format("Error while controlling room %s %s: %s", controllableProperty, cachedRooms.get(roomIndex), e.getMessage()), e);
		}
	}

	/**
	 * This method is used for calling control room properties:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @throws IllegalStateException when fail to control
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
				throw new IllegalStateException(String.format("Run Scene %s fail, please try again later", controllableProperty));
			}
			addAdvanceControlProperties(advancedControllableProperties,
					createButton(stats, AggregatorGroupControllingMetric.SCENE.getName().concat(controllableProperty), SmartThingsConstant.SUCCESSFUL, SmartThingsConstant.SUCCESSFUL));
			isEmergencyDelivery = true;
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Error while controlling scene %s: %s", controllableProperty, e.getMessage()), e);
		}
	}

	/**
	 * This method is used for calling control create room properties:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param value value of controllable property
	 * @throws IllegalStateException when fail to control
	 */
	private void createRoomControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, String value) {
		CreateRoomMetric createRoomMetric = CreateRoomMetric.getByName(controllableProperty);
		cachedCreateRoom.setLocationId(locationIdFiltered);

		isEditedForCreateRoom = true;
		switch (createRoomMetric) {
			case ROOM_NAME:
				for (Room room : cachedRooms) {
					if (room.getName().equalsIgnoreCase(value)) {
						throw new IllegalArgumentException(String.format("The room name %s already exists, Please chose the different room name", value));
					}
				}
				cachedCreateRoom.setName(value);
				populateCreateRoomManagement(stats, advancedControllableProperties);
				break;
			case CANCEL:
				cachedCreateRoom = new Room();
				break;
			case CREATE_ROOM:
				String roomName = cachedCreateRoom.getName();
				if (roomName == null || roomName.trim().isEmpty()) {
					throw new IllegalArgumentException("Invalid room name, the room name can not be empty");
				}
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
						throw new IllegalStateException(String.format("Creating room with name %s fail, please try again later", value));
					}
					isEditedForCreateRoom = false;
					populateCreateRoomManagement(stats, advancedControllableProperties);
					populateRoomManagement(stats, advancedControllableProperties);
					isEmergencyDelivery = true;
				} catch (Exception e) {
					throw new IllegalStateException(String.format("Error while controlling create room %s: %s", controllableProperty, e.getMessage()), e);
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

	//region populate and perform device dashboard control
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used to populate scenes trigger group
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 */
	private void populateDeviceDashboardView(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		List<String> rooms;

		boolean isNoRoomAssignedExisting = validateNoRoomAssigned();
		// Get list room modes when applying filter
		if (StringUtils.isNotNullOrEmpty(roomFilter)) {
			Set<String> filteredRoom = convertUserInput(roomFilter.toUpperCase());
			rooms = cachedRooms.stream().map(Room::getName).filter(roomName -> filteredRoom.contains(roomName.toUpperCase())).collect(Collectors.toList());
			if (filteredRoom.contains(SmartThingsConstant.NO_ROOM_ASSIGNED.toUpperCase()) && isNoRoomAssignedExisting) {
				rooms.add(SmartThingsConstant.NO_ROOM_ASSIGNED);
			}
		} else {
			// Get list room modes when do not applying filter
			rooms = cachedRooms.stream().map(Room::getName).collect(Collectors.toList());
			if (!deviceIds.isEmpty() && isNoRoomAssignedExisting) {
				rooms.add(SmartThingsConstant.NO_ROOM_ASSIGNED);
			}
		}
		if (!rooms.isEmpty() && (StringUtils.isNullOrEmpty(currentRoomInDeviceDashBoard) || !rooms.contains(currentRoomInDeviceDashBoard))) {
			currentRoomInDeviceDashBoard = rooms.get(0);
		}
		String currentRoomId = findRoomIdByName(currentRoomInDeviceDashBoard);

		// remove unused control
		if (!unusedDeviceControlKeys.isEmpty()) {
			removeUnusedStatsAndControls(stats, advancedControllableProperties, unusedDeviceControlKeys);
		}
		boolean isEmptyDevicesControl = true;

		// populate ActiveRoom dropdown list control
		if (!rooms.isEmpty()) {
			addAdvanceControlProperties(advancedControllableProperties,
					createDropdown(stats, AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + RoomManagementMetric.ACTIVE_ROOM.getName(), rooms, currentRoomInDeviceDashBoard));
		}

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
							String currentValue = getDefaultValueForNullData(action.getStandbyPowerSwitch().getValue(), SmartThingsConstant.OFF);
							if (currentValue.contains(AggregatedDeviceDropdownListModesControllingConstant.WINDOW_SHADE_MODE_OPEN)) {
								currentValue = AggregatedDeviceDropdownListModesControllingConstant.WINDOW_SHADE_MODE_OPEN;
							}
							if (currentValue.contains(AggregatedDeviceDropdownListModesControllingConstant.WINDOW_SHADE_MODE_CLOSE)) {
								currentValue = AggregatedDeviceDropdownListModesControllingConstant.WINDOW_SHADE_MODE_CLOSE;
							}

							addAdvanceControlProperties(advancedControllableProperties,
									createSwitch(stats, AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + device.getName(), currentValue, offLabel, onLabel));
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
			stats.put(AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + AggregatorGroupControllingMetric.MESSAGE.getName(), "There are no devices available in this room or location");
		} else {
			stats.remove(AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + AggregatorGroupControllingMetric.MESSAGE.getName());
		}
	}

	/**
	 * This method is used to validate if the locations contain the No Room Assigned
	 *
	 * @return boolean is contains No Room Assigned
	 */
	private boolean validateNoRoomAssigned() {
		for (Device device : cachedDevices.values()) {
			if (StringUtils.isNullOrEmpty(device.getRoomId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method is used for calling control device dashboard properties:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param value value of controllable property
	 * @throws IllegalStateException when fail to control
	 */
	private void deviceDashboardControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, String value) {
		// ActiveRoom dropdown control
		if (controllableProperty.equals(RoomManagementMetric.ACTIVE_ROOM.getName())) {
			currentRoomInDeviceDashBoard = value;
			populateDeviceDashboardView(stats, advancedControllableProperties);
			isEmergencyDelivery = true;
		} else {
			// devices control
			try {
				Device device = findDeviceByName(controllableProperty);
				if (!convertDeviceStatusValue(device)) {
					throw new IllegalStateException(String.format("Unable to control %s, device is offline", device.getName()));
				}
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
						requestBody = device.contributeRequestBodyForNonParamCommand(action.getCapability(), command);
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

									String deviceId = device.getDeviceId();
									// device need 1s to update the new status
									Thread.sleep(1000);
									retrieveDeviceFullStatus(deviceId);

									Device cachedDevice = new Device(cachedDevices.get(device.getDeviceId()));
									cachedDevicesAfterPollingInterval.put(deviceId, cachedDevice);
									AggregatedDevice aggregatedDevice = cachedAggregatedDevices.get(deviceId);
									Map<String, String> aggregatedDeviceStats = aggregatedDevice.getProperties();
									List<AdvancedControllableProperty> aggregatedDeviceAdvancedControllablePropertyList = aggregatedDevice.getControllableProperties();
									populateAggregatedDeviceView(aggregatedDeviceStats, aggregatedDeviceAdvancedControllablePropertyList, cachedDevice);

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
						throw new IllegalStateException(String.format("control device %s fail, please try again later", controllableProperty));
					}

					populateDeviceDashboardView(stats, advancedControllableProperties);
					isEmergencyDelivery = true;
				} else {
					throw new IllegalArgumentException(String.format("can not find device: %s", controllableProperty));
				}
			} catch (Exception e) {
				throw new IllegalStateException(String.format("Error while controlling device %s: %s", controllableProperty, e.getMessage()), e);
			}
		}

	}

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region populate aggregated device
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used to populate color control group
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 */
	private void populateAggregatedDeviceView(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, Device device) {
		populateAggregatedDeviceRoomControl(stats, advancedControllableProperties, device);

		Optional<List<DetailViewPresentation>> detailViewPresentations = Optional.ofNullable(device.getPresentation())
				.map(DevicePresentation::getDetailViewPresentations);

		if (detailViewPresentations.isPresent() && !detailViewPresentations.get().isEmpty()) {
			for (DetailViewPresentation detailViewPresentation : detailViewPresentations.get()) {

				// populate specific controllable property of detail view
				switch (detailViewPresentation.getCapability()) {
					case AggregatedDeviceColorControllingConstant.COLOR_CONTROL:
						ColorDevicePresentation colorDevicePresentation = Optional.ofNullable(device.getPresentation())
								.map(DevicePresentation::getColor)
								.orElse(new ColorDevicePresentation(AggregatedDeviceColorControllingConstant.HUE_COORDINATE, AggregatedDeviceColorControllingConstant.MIN_SATURATION,
										AggregatedDeviceColorControllingConstant.CUSTOM_COLOR));

						// populate color dropdown control
						List<String> colorModes = commonColors.keySet().stream().collect(Collectors.toList());
						colorModes.add(AggregatedDeviceColorControllingConstant.CUSTOM_COLOR);
						String currentColor = Optional.ofNullable(colorDevicePresentation.getCurrentColor()).orElse(SmartThingsConstant.EMPTY);

						if (SmartThingsConstant.EMPTY.equals(currentColor)) {
							currentColor = getDefaultColorNameByHueAndSaturation(colorDevicePresentation.getHue(), colorDevicePresentation.getSaturation());
						}
						addAdvanceControlProperties(advancedControllableProperties,
								createDropdown(stats, AggregatedDeviceControllingMetric.COLOR_CONTROL.getName(), colorModes, currentColor));

						// populate custom HSV color control
						String hueControlLabel = AggregatedDeviceControllingMetric.HUE_CONTROL.getName();
						String currentHueControlLabel = AggregatedDeviceControllingMetric.HUE_CONTROL.getName()
								+ AggregatedDeviceControllingMetric.CURRENT_VALUE.getName();
						String saturationLabel = AggregatedDeviceControllingMetric.SATURATION_CONTROL.getName();
						String currentSaturationControlLabel = AggregatedDeviceControllingMetric.SATURATION_CONTROL.getName()
								+ AggregatedDeviceControllingMetric.CURRENT_VALUE.getName() + SmartThingsConstant.PERCENT_UNIT;

						if (AggregatedDeviceColorControllingConstant.CUSTOM_COLOR.equals(currentColor)) {
							String hueLabelStart = String.valueOf((int) AggregatedDeviceColorControllingConstant.MIN_HUE);
							String hueLabelEnd = String.valueOf((int) AggregatedDeviceColorControllingConstant.MAX_HUE);
							String saturationLabelStart = String.valueOf((int) AggregatedDeviceColorControllingConstant.MIN_SATURATION);
							String saturationLabelEnd = String.valueOf((int) AggregatedDeviceColorControllingConstant.MAX_SATURATION);
							String colorName = getColorNameByHueAndSaturation(colorDevicePresentation.getHue(), colorDevicePresentation.getSaturation());

							addAdvanceControlProperties(advancedControllableProperties,
									createSlider(stats, hueControlLabel, hueLabelStart, hueLabelEnd, AggregatedDeviceColorControllingConstant.MIN_HUE, AggregatedDeviceColorControllingConstant.MAX_HUE,
											convertHueToRadianValue(colorDevicePresentation.getHue())));
							addAdvanceControlProperties(advancedControllableProperties,
									createSlider(stats, saturationLabel, saturationLabelStart, saturationLabelEnd, AggregatedDeviceColorControllingConstant.MIN_SATURATION,
											AggregatedDeviceColorControllingConstant.MAX_SATURATION,
											colorDevicePresentation.getSaturation()));
							stats.put(currentHueControlLabel, String.valueOf(colorDevicePresentation.getHue().intValue()));
							stats.put(currentSaturationControlLabel, String.valueOf(colorDevicePresentation.getSaturation().intValue()));
							stats.put(AggregatedDeviceControllingMetric.CURRENT_COLOR_CONTROL.getName(), colorName);
						} else {
							Set<String> unusedKeys = new HashSet<>();
							unusedKeys.add(hueControlLabel);
							unusedKeys.add(saturationLabel);
							unusedKeys.add(currentHueControlLabel);
							unusedKeys.add(currentSaturationControlLabel);
							unusedKeys.add(AggregatedDeviceControllingMetric.CURRENT_COLOR_CONTROL.getName());
							removeUnusedStatsAndControls(stats, advancedControllableProperties, unusedKeys);
						}
						continue;
					default:
						// we do not log error/ warning here, we are going to populate data for common detail view controllable properties in the next step
						break;
				}
				populateCommonDetailViewControllableProperties(stats, advancedControllableProperties, detailViewPresentation);
			}
		}
	}

	/**
	 * This method is used for get color default name by Hue and Saturation:
	 *
	 * @param hue color hue value
	 * @param saturation color saturation value
	 */
	private String getDefaultColorNameByHueAndSaturation(float hue, float saturation) {
		Color color = Color.getHSBColor(convertHueToPercentValue(hue), convertSaturationToPercentValue(saturation), AggregatedDeviceColorControllingConstant.DEFAULT_BRIGHTNESS);
		if (color.equals(Color.RED)) {
			return AggregatedDeviceColorControllingConstant.RED;
		}
		if (color.equals(Color.CYAN)) {
			return AggregatedDeviceColorControllingConstant.CYAN;
		}
		if (color.equals(Color.GREEN)) {
			return AggregatedDeviceColorControllingConstant.GREEN;
		}
		if (color.equals(Color.ORANGE)) {
			return AggregatedDeviceColorControllingConstant.ORANGE;
		}
		if (color.equals(Color.PINK)) {
			return AggregatedDeviceColorControllingConstant.PINK;
		}
		if (color.equals(Color.BLUE)) {
			return AggregatedDeviceColorControllingConstant.BLUE;
		}
		if (color.equals(Color.WHITE)) {
			return AggregatedDeviceColorControllingConstant.WHITE;
		}
		if (color.equals(Color.YELLOW)) {
			return AggregatedDeviceColorControllingConstant.YELLOW;
		}
		return AggregatedDeviceColorControllingConstant.CUSTOM_COLOR;
	}

	/**
	 * This method is used to populate common device control
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 * @param detailViewPresentation detail vew presentation
	 */
	private void populateCommonDetailViewControllableProperties(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties,
			DetailViewPresentation detailViewPresentation) {
		switch (detailViewPresentation.getDisplayType()) {
			case DeviceDisplayTypesMetric.STAND_BY_POWER_SWITCH:
			case DeviceDisplayTypesMetric.TOGGLE_SWITCH:
			case DeviceDisplayTypesMetric.SWITCH:
				String currentValue = detailViewPresentation.getStandbyPowerSwitch().getValue();
				if (StringUtils.isNullOrEmpty(currentValue)) {
					break;
				}
				String onLabel = getDefaultValueForNullData(detailViewPresentation.getStandbyPowerSwitch().getCommand().getOn(), SmartThingsConstant.ON);
				String offLabel = getDefaultValueForNullData(detailViewPresentation.getStandbyPowerSwitch().getCommand().getOff(), SmartThingsConstant.OFF);

				// check on/off status for special case
				if (SmartThingsConstant.TV_AUDIO_MUTE_CAPABILITY.equals(detailViewPresentation.getCapability())) {
					currentValue = currentValue.equals(SmartThingsConstant.TV_AUDIO_MUTE) ? onLabel : offLabel;
				}

				addAdvanceControlProperties(advancedControllableProperties,
						createSwitch(stats, convertToTitleCaseIteratingChars(detailViewPresentation.getLabel()), currentValue, offLabel, onLabel));
				break;
			case DeviceDisplayTypesMetric.PUSH_BUTTON:
				if (!isEmergencyDelivery) {
					addAdvanceControlProperties(advancedControllableProperties,
							createButton(stats, convertToTitleCaseIteratingChars(detailViewPresentation.getLabel()), SmartThingsConstant.PUSH, SmartThingsConstant.PUSHING));
				}
				break;
			case DeviceDisplayTypesMetric.SLIDER:
				Optional<String> command = Optional.ofNullable(detailViewPresentation.getSlider()).map(Slider::getCommand);
				Optional<List<Float>> range = Optional.ofNullable(detailViewPresentation.getSlider())
						.map(Slider::getRange);

				if (range.isPresent() && range.get().size() == 2) {
					if (StringUtils.isNullOrEmpty(detailViewPresentation.getSlider().getValue())) {
						break;
					}
					Float currentSliderValue = Float.parseFloat(getDefaultValueForNullData(detailViewPresentation.getSlider().getValue(), range.get().get(0).toString()));
					String unit = detailViewPresentation.getSlider().getUnit();
					Float valueStart = range.get().get(0);
					Float valueEnd = range.get().get(1);

					if ((AggregatedDeviceControllingMetric.THERMOSTAT_HEATING_SET_POINT.getName().equals(detailViewPresentation.getCapability()) ||
							AggregatedDeviceControllingMetric.THERMOSTAT_COOLING_SET_POINT.getName().equals(detailViewPresentation.getCapability()) ||
							AggregatedDeviceControllingMetric.PARTY_VOICE23922_VTEMPSET.getName().equals(detailViewPresentation.getCapability())) &&
							SmartThingsConstant.FAHRENHEIT.equalsIgnoreCase(unit)) {
						valueStart = convertFromCelsiusToFahrenheit(valueStart);
						valueEnd = convertFromCelsiusToFahrenheit(valueEnd);
					}

					String labelStart = String.valueOf(valueStart.intValue()).concat(unit);
					String labelEnd = String.valueOf(valueEnd.intValue()).concat(unit);
					String controlLabel = convertToTitleCaseIteratingChars(detailViewPresentation.getLabel());

					// populate read-only property when controllable property does not provide control command
					if (!command.isPresent()) {
						stats.put(controlLabel + SmartThingsConstant.LEFT_PARENTHESES + unit + SmartThingsConstant.RIGHT_PARENTHESES, String.valueOf(currentSliderValue.intValue()));
						break;
					}

					addAdvanceControlProperties(advancedControllableProperties, createSlider(stats, controlLabel, labelStart, labelEnd, valueStart, valueEnd, currentSliderValue));
					stats.put(controlLabel + AggregatedDeviceControllingMetric.CURRENT_VALUE.getName() + SmartThingsConstant.LEFT_PARENTHESES + unit + SmartThingsConstant.RIGHT_PARENTHESES,
							String.valueOf(currentSliderValue.intValue()));
				}
				break;
			case DeviceDisplayTypesMetric.LIST:
				Optional<List<Alternative>> alternatives = Optional.ofNullable(detailViewPresentation.getDropdownList())
						.map(DropdownList::getCommand)
						.map(Command::getAlternatives);

				if (alternatives.isPresent()) {
					List<String> dropdownListModes = new ArrayList<>();
					currentValue = detailViewPresentation.getDropdownList().getValue();
					if (StringUtils.isNullOrEmpty(currentValue)) {
						break;
					}

					switch (detailViewPresentation.getCapability()) {
						case AggregatedDeviceDropdownListModesControllingConstant.THERMOSTAT_FAN_MODE:
							dropdownListModes.add(AggregatedDeviceDropdownListModesControllingConstant.THERMOSTAT_FAN_MODE_AUTO);
							dropdownListModes.add(AggregatedDeviceDropdownListModesControllingConstant.THERMOSTAT_FAN_MODE_CIRCULATE);
							dropdownListModes.add(AggregatedDeviceDropdownListModesControllingConstant.THERMOSTAT_FAN_MODE_ON);
							break;
						case AggregatedDeviceDropdownListModesControllingConstant.THERMOSTAT_MODE:
							dropdownListModes.add(AggregatedDeviceDropdownListModesControllingConstant.THERMOSTAT_MODE_OFF);
							dropdownListModes.add(AggregatedDeviceDropdownListModesControllingConstant.THERMOSTAT_MODE_HEAT);
							dropdownListModes.add(AggregatedDeviceDropdownListModesControllingConstant.THERMOSTAT_MODE_AUTO);
							dropdownListModes.add(AggregatedDeviceDropdownListModesControllingConstant.THERMOSTAT_MODE_COOL);
							dropdownListModes.add(AggregatedDeviceDropdownListModesControllingConstant.THERMOSTAT_MODE_EMERGENCY);
							break;
						case AggregatedDeviceDropdownListModesControllingConstant.WINDOW_SHADE_MODE:
							dropdownListModes.add(AggregatedDeviceDropdownListModesControllingConstant.WINDOW_SHADE_MODE_OPEN);
							dropdownListModes.add(AggregatedDeviceDropdownListModesControllingConstant.WINDOW_SHADE_MODE_CLOSE);
							if (currentValue.contains(AggregatedDeviceDropdownListModesControllingConstant.WINDOW_SHADE_MODE_OPEN)) {
								currentValue = AggregatedDeviceDropdownListModesControllingConstant.WINDOW_SHADE_MODE_OPEN;
							}
							if (currentValue.contains(AggregatedDeviceDropdownListModesControllingConstant.WINDOW_SHADE_MODE_CLOSE)) {
								currentValue = AggregatedDeviceDropdownListModesControllingConstant.WINDOW_SHADE_MODE_CLOSE;
							}
							break;
						case AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_MODE:
							String playBackStatus = Optional.ofNullable(detailViewPresentation.getDropdownList().getState()).map(State::getValue)
									.orElse(AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_STATUS_PLAY);
							currentValue = mediaPlaybackModes.get(playBackStatus);
							dropdownListModes = alternatives.get().stream().map(Alternative::getKey)
									.collect(Collectors.toList());
							break;
						default:
							dropdownListModes = alternatives.get().stream().map(Alternative::getKey)
									.collect(Collectors.toList());
							break;
					}

					addAdvanceControlProperties(advancedControllableProperties,
							createDropdown(stats, convertToTitleCaseIteratingChars(detailViewPresentation.getLabel()), dropdownListModes, currentValue));
				}
				break;
			case DeviceDisplayTypesMetric.NUMBER_FIELD:
				currentValue = detailViewPresentation.getNumberField().getValue();
				if (StringUtils.isNullOrEmpty(currentValue)) {
					break;
				}
				addAdvanceControlProperties(advancedControllableProperties,
						createNumeric(stats, convertToTitleCaseIteratingChars(detailViewPresentation.getLabel()), currentValue));
				break;
			case DeviceDisplayTypesMetric.TEXT_FIELD:
				currentValue = detailViewPresentation.getTextField().getValue();
				if (StringUtils.isNullOrEmpty(currentValue)) {
					break;
				}
				if (SmartThingsConstant.TV_CHANNEL.equals(detailViewPresentation.getCapability())) {
					stats.put(detailViewPresentation.getLabel(), currentValue);
					break;
				}
				addAdvanceControlProperties(advancedControllableProperties,
						createText(stats, convertToTitleCaseIteratingChars(detailViewPresentation.getLabel()), currentValue));
				break;
			default:
				if (logger.isWarnEnabled()) {
					logger.warn(String.format("Unexpected device display type: %s", detailViewPresentation.getDisplayType()));
				}
				break;
		}
	}

	/**
	 * This method is used to Convert from celsius to fahrenheit
	 */
	private float convertFromCelsiusToFahrenheit(float value) {
		return value * 9 / 5 + 32;
	}

	/**
	 * This method is used to init mediaPlaybackModes
	 */
	private void initMediaPlayBackList() {
		mediaPlaybackModes.put(AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_STATUS_PLAY, AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_MODE_PLAY);
		mediaPlaybackModes.put(AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_STATUS_PAUSE, AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_MODE_PAUSE);
		mediaPlaybackModes.put(AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_STATUS_REWIND, AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_MODE_REWIND);
		mediaPlaybackModes.put(AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_STATUS_FAST_FORWARD,
				AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_MODE_FAST_FORWARD);
		mediaPlaybackModes.put(AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_STATUS_STOP, AggregatedDeviceDropdownListModesControllingConstant.TV_MEDIA_PLAYBACK_MODE_STOP);
	}

	/**
	 * This method is used to populate common device control
	 *
	 * @param stats store all statistics
	 * @param advancedControllableProperties store all controllable properties
	 * @param device device data
	 */
	private void populateAggregatedDeviceRoomControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, Device device) {
		List<String> roomModes = cachedRooms.stream().map(Room::getName).collect(Collectors.toList());
		if (validateNoRoomAssigned()) {
			roomModes.add(SmartThingsConstant.NO_ROOM_ASSIGNED);
		}
		String currentRoom = findRoomNameById(device.getRoomId());
		if (currentRoom.isEmpty()) {
			currentRoom = SmartThingsConstant.NO_ROOM_ASSIGNED;
		}
		addAdvanceControlProperties(advancedControllableProperties,
				createDropdown(stats, AggregatedDeviceControllingMetric.ROOM_MANAGEMENT.getName(), roomModes, currentRoom));

		populateCurrentLocation(stats);
	}

	/**
	 * This method is used for calling room control for aggregated device:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param value value of controllable property
	 * @throws IllegalStateException when fail to control
	 */
	private void aggregatedDeviceRoomControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, String value, String
			deviceId) {
		try {
			Device device = cachedDevicesAfterPollingInterval.get(deviceId);
			String roomID = findRoomIdByName(value);
			if (device != null && StringUtils.isNotNullOrEmpty(roomID)) {
				String request = SmartThingsURL.DEVICES
						.concat(SmartThingsConstant.SLASH)
						.concat(device.getDeviceId());
				String requestBody = device.contributeRequestBodyForUpdateDevice(roomID);
				HttpHeaders headers = new HttpHeaders();
				ResponseEntity<?> response = doRequest(request, HttpMethod.PUT, headers, requestBody, String.class);

				handleRateLimitExceed(response);

				Optional<?> responseBody = Optional.ofNullable(response)
						.map(HttpEntity::getBody);
				if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
					device.setRoomId(roomID);
					populateAggregatedDeviceRoomControl(stats, advancedControllableProperties, device);
					cachedDevicesAfterPollingInterval.put(deviceId, device);
					cachedDevices.put(deviceId, device);
					isEmergencyDelivery = true;
				} else {
					throw new IllegalStateException(String.format("can not assign device to room %s", value));
				}
			} else {
				throw new IllegalStateException(String.format("can not assign device to room %s", value));
			}
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Error while controlling device %s: %s", controllableProperty, e.getMessage()), e);
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region perform aggregated device color control
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used for calling color dropdown control for aggregated device:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param value value of controllable property
	 * @throws ResourceNotReachableException when fail to control
	 */
	private void aggregatedDeviceColorDropdownControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, String value,
			String deviceId) {
		Color color = commonColors.get(value);
		Device device = cachedDevicesAfterPollingInterval.get(deviceId);

		if (color != null) {
			float[] hsvColor = color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
			String hue = String.valueOf(hsvColor[0] * AggregatedDeviceColorControllingConstant.ONE_HUNDRED_PERCENT);
			String saturation = String.valueOf(convertSaturationToSmartThingsValue(hsvColor[1]));

			sendColorControlRequest(controllableProperty, device, hue, saturation);

			device.getPresentation().getColor().setCurrentColor(value);
			device.getPresentation().getColor().setHue(Float.parseFloat(hue));
			device.getPresentation().getColor().setSaturation(Float.parseFloat(saturation));
			isEmergencyDelivery = true;
		} else {
			ColorDevicePresentation colorDevicePresentation = Optional.ofNullable(device.getPresentation()).map(DevicePresentation::getColor)
					.orElse(new ColorDevicePresentation(AggregatedDeviceColorControllingConstant.HUE_COORDINATE, AggregatedDeviceColorControllingConstant.MIN_SATURATION,
							AggregatedDeviceColorControllingConstant.CUSTOM_COLOR));
			colorDevicePresentation.setCurrentColor(AggregatedDeviceColorControllingConstant.CUSTOM_COLOR);
			device.getPresentation().setColor(colorDevicePresentation);
		}
		cachedDevicesAfterPollingInterval.put(deviceId, device);
		cachedDevices.put(deviceId, device);
		populateAggregatedDeviceView(stats, advancedControllableProperties, device);
	}

	/**
	 * This method is used for calling color dropdown control for aggregated device:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param hue value of hue
	 * @throws ResourceNotReachableException when fail to control
	 */
	private void aggregatedDeviceColorHueControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, Float hue,
			String deviceId) {
		Device device = cachedDevicesAfterPollingInterval.get(deviceId);

		float saturation = Optional.ofNullable(device.getPresentation()).map(DevicePresentation::getColor).map(ColorDevicePresentation::getSaturation)
				.orElse(AggregatedDeviceColorControllingConstant.MIN_SATURATION);

		sendColorControlRequest(controllableProperty, device, hue.toString(), String.valueOf(saturation));

		device.getPresentation().getColor().setHue(Float.parseFloat(hue.toString()));
		cachedDevicesAfterPollingInterval.put(deviceId, device);
		cachedDevices.put(deviceId, device);
		populateAggregatedDeviceView(stats, advancedControllableProperties, device);
		isEmergencyDelivery = true;
	}

	/**
	 * This method is used for calling color dropdown control for aggregated device:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param saturation value of saturation
	 */
	private void aggregatedDeviceColorSaturationControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, Float
			saturation,
			String deviceId) {
		Device device = cachedDevicesAfterPollingInterval.get(deviceId);
		float hue = Optional.ofNullable(device.getPresentation()).map(DevicePresentation::getColor).map(ColorDevicePresentation::getHue).orElse(AggregatedDeviceColorControllingConstant.MIN_HUE);

		sendColorControlRequest(controllableProperty, device, String.valueOf(hue), saturation.toString());

		device.getPresentation().getColor().setSaturation(Float.parseFloat(saturation.toString()));
		cachedDevicesAfterPollingInterval.put(deviceId, device);
		cachedDevices.put(deviceId, device);
		populateAggregatedDeviceView(stats, advancedControllableProperties, device);
		isEmergencyDelivery = true;
	}

	/**
	 * This method is used for sending color control request:
	 *
	 * @param controllableProperty name of controllable property
	 * @param device device data
	 * @param hue color hue
	 * @param saturation color saturation
	 * @throws IllegalStateException when fail to control
	 */
	private boolean sendColorControlRequest(String controllableProperty, Device device, String hue, String saturation) {
		try {
			if (device != null) {
				String request = SmartThingsURL.DEVICES
						.concat(SmartThingsConstant.SLASH)
						.concat(device.getDeviceId())
						.concat(SmartThingsURL.COMMANDS);

				HttpHeaders headers = new HttpHeaders();

				String requestBody = device.contributeRequestBodyForColorCommand(hue, saturation);

				ResponseEntity<?> response = doRequest(request, HttpMethod.POST, headers, requestBody, String.class);

				handleRateLimitExceed(response);
				if (!response.getStatusCode().is2xxSuccessful()) {
					throw new IllegalStateException(String.format("control device %s fail, please try again later", controllableProperty));
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Error while controlling device %s: %s", controllableProperty, e.getMessage()), e);
		}
		return true;
	}

	/**
	 * This method is used to convert hue from smartThings value to radian value
	 *
	 * @param hue color hue value
	 * @return Float hue value
	 */
	private Float convertHueToRadianValue(float hue) {
		return hue * AggregatedDeviceColorControllingConstant.MAX_HUE / AggregatedDeviceColorControllingConstant.ONE_HUNDRED_PERCENT;
	}

	/**
	 * This method is used to convert hue from smartThings value to percent
	 *
	 * @param hue color hue value
	 * @return Float saturation value
	 */
	private Float convertHueToPercentValue(float hue) {
		return hue / AggregatedDeviceColorControllingConstant.ONE_HUNDRED_PERCENT;
	}

	/**
	 * This method is used to convert hue from smartThings value to percent
	 *
	 * @param saturation color saturation value
	 * @return Float saturation value
	 */
	private Float convertSaturationToPercentValue(float saturation) {
		return saturation / AggregatedDeviceColorControllingConstant.ONE_HUNDRED_PERCENT;
	}

	/**
	 * This method is used to convert hue from radian value to smartThings value
	 *
	 * @param hue color hue value
	 * @return Float hue value
	 */
	private Float convertHueToSmartThingsValue(float hue) {
		return hue * AggregatedDeviceColorControllingConstant.ONE_HUNDRED_PERCENT / AggregatedDeviceColorControllingConstant.MAX_HUE;
	}

	/**
	 * This method is used to convert hue from to percent value to smartThings value
	 *
	 * @param saturation color saturation value
	 * @return Float saturation value
	 */
	private Float convertSaturationToSmartThingsValue(float saturation) {
		return saturation * AggregatedDeviceColorControllingConstant.ONE_HUNDRED_PERCENT;
	}

	/**
	 * This method is used for get color name by Hue and Saturation:
	 *
	 * @param hue color hue value
	 * @param saturation color saturation value
	 */
	private String getColorNameByHueAndSaturation(float hue, float saturation) {
		Color color = Color.getHSBColor(convertHueToPercentValue(hue), convertSaturationToPercentValue(saturation), AggregatedDeviceColorControllingConstant.DEFAULT_BRIGHTNESS);
		String colorName =
				SmartThingsConstant.LEFT_PARENTHESES + color.getRed() + SmartThingsConstant.COMMA + color.getGreen() + SmartThingsConstant.COMMA + color.getBlue() + SmartThingsConstant.RIGHT_PARENTHESES;
		hue = convertHueToRadianValue(hue);
		if (hue >= AggregatedDeviceColorControllingConstant.HUE_COORDINATE && hue < AggregatedDeviceColorControllingConstant.REDS_RANGE) {
			return AggregatedDeviceColorControllingConstant.RED_SECTION + colorName;
		}
		if (hue >= AggregatedDeviceColorControllingConstant.REDS_RANGE && hue < AggregatedDeviceColorControllingConstant.ORANGES_RANGE) {
			return AggregatedDeviceColorControllingConstant.ORANGE_SECTION + colorName;
		}
		if (hue >= AggregatedDeviceColorControllingConstant.ORANGES_RANGE && hue < AggregatedDeviceColorControllingConstant.YELLOWS_RANGE) {
			return AggregatedDeviceColorControllingConstant.YELLOW_SECTION + colorName;
		}
		if (hue >= AggregatedDeviceColorControllingConstant.YELLOWS_RANGE && hue < AggregatedDeviceColorControllingConstant.YELLOW_GREENS_RANGE) {
			return AggregatedDeviceColorControllingConstant.YELLOW_GREEN_SECTION + colorName;
		}
		if (hue >= AggregatedDeviceColorControllingConstant.YELLOW_GREENS_RANGE && hue < AggregatedDeviceColorControllingConstant.GREENS_RANGE) {
			return AggregatedDeviceColorControllingConstant.GREEN_SECTION + colorName;
		}
		if (hue >= AggregatedDeviceColorControllingConstant.GREENS_RANGE && hue < AggregatedDeviceColorControllingConstant.BLUE_GREENS_RANGE) {
			return AggregatedDeviceColorControllingConstant.BLUE_GREEN_SECTION + colorName;
		}
		if (hue >= AggregatedDeviceColorControllingConstant.BLUE_GREENS_RANGE && hue < AggregatedDeviceColorControllingConstant.BLUES_RANGE) {
			return AggregatedDeviceColorControllingConstant.BLUE_SECTION + colorName;
		}
		if (hue >= AggregatedDeviceColorControllingConstant.BLUES_RANGE && hue < AggregatedDeviceColorControllingConstant.BLUE_VIOLETS_RANGE) {
			return AggregatedDeviceColorControllingConstant.BLUE_VIOLET_SECTION + colorName;
		}
		if (hue >= AggregatedDeviceColorControllingConstant.BLUE_VIOLETS_RANGE && hue < AggregatedDeviceColorControllingConstant.VIOLETS_RANGE) {
			return AggregatedDeviceColorControllingConstant.VIOLET_SECTION + colorName;
		}
		if (hue >= AggregatedDeviceColorControllingConstant.VIOLETS_RANGE && hue < AggregatedDeviceColorControllingConstant.MAUVES_RANGE) {
			return AggregatedDeviceColorControllingConstant.MAUVE_SECTION + colorName;
		}
		if (hue >= AggregatedDeviceColorControllingConstant.MAUVES_RANGE && hue < AggregatedDeviceColorControllingConstant.MAUVE_PINKS_RANGE) {
			return AggregatedDeviceColorControllingConstant.MAUVE_PINK_SECTION + colorName;
		}
		if (hue >= AggregatedDeviceColorControllingConstant.MAUVES_RANGE && hue < AggregatedDeviceColorControllingConstant.PINKS_RANGE) {
			return AggregatedDeviceColorControllingConstant.PINK_SECTION + colorName;
		}
		return colorName;
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region perform aggregated device common control
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used for calling control device dashboard properties:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param value value of controllable property
	 * @throws IllegalArgumentException when fail to control
	 */
	private void aggregatedDeviceControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty, String value, String deviceId) {

		Device device = cachedDevicesAfterPollingInterval.get(deviceId);
		if (device != null) {
			Optional<List<DetailViewPresentation>> detailViewPresentations = Optional.ofNullable(device.getPresentation())
					.map(DevicePresentation::getDetailViewPresentations);
			if (detailViewPresentations.isPresent()) {
				DetailViewPresentation detailViewPresentation = findControlByLabel(controllableProperty, detailViewPresentations.get());
				if (detailViewPresentation != null) {
					switch (detailViewPresentation.getDisplayType()) {
						case DeviceDisplayTypesMetric.STAND_BY_POWER_SWITCH:
						case DeviceDisplayTypesMetric.TOGGLE_SWITCH:
						case DeviceDisplayTypesMetric.SWITCH:
							switchControl(stats, advancedControllableProperties, value, controllableProperty, device, detailViewPresentation);
							break;
						case DeviceDisplayTypesMetric.PUSH_BUTTON:
							pushButtonControl(stats, advancedControllableProperties, controllableProperty, device, detailViewPresentation);
							break;
						case DeviceDisplayTypesMetric.SLIDER:
						case DeviceDisplayTypesMetric.SWITCH_LEVEL:
							sliderControl(stats, advancedControllableProperties, value, controllableProperty, device, detailViewPresentation);
							break;
						case DeviceDisplayTypesMetric.LIST:
							listControl(stats, advancedControllableProperties, value, controllableProperty, device, detailViewPresentation);
							break;
						case DeviceDisplayTypesMetric.NUMBER_FIELD:
							if (StringUtils.isNotNullOrEmpty(value)) {
								numberControl(stats, advancedControllableProperties, value, controllableProperty, device, detailViewPresentation);
							}
							break;
						case DeviceDisplayTypesMetric.TEXT_FIELD:
							textControl(stats, advancedControllableProperties, value, controllableProperty, device, detailViewPresentation);
							break;
						default:
							if (logger.isWarnEnabled()) {
								logger.warn(String.format("Unexpected device display type: %s", detailViewPresentation.getDisplayType()));
							}
							break;
					}
				}
				isEmergencyDelivery = true;
			} else {
				throw new IllegalArgumentException(String.format("control %s error: can not find device", controllableProperty));
			}
		}
	}

	/**
	 * This method is used for calling switch control:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param value value of controllable property
	 * @param controllableProperty name of controllable property
	 * @param device device data
	 * @param detailViewPresentation device detailViewPresentation
	 * @throws IllegalStateException when fail to control
	 */
	private void switchControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String value, String controllableProperty,
			Device device, DetailViewPresentation detailViewPresentation) {
		try {
			if (device != null) {
				String request = SmartThingsURL.DEVICES
						.concat(SmartThingsConstant.SLASH)
						.concat(device.getDeviceId())
						.concat(SmartThingsURL.COMMANDS);

				HttpHeaders headers = new HttpHeaders();

				String command = value.equals("1") ? detailViewPresentation.getStandbyPowerSwitch().getCommand().getOn() : detailViewPresentation.getStandbyPowerSwitch().getCommand().getOff();
				String requestBody = device.contributeRequestBodyForNonParamCommand(detailViewPresentation.getCapability(), command);

				ResponseEntity<?> response = doRequest(request, HttpMethod.POST, headers, requestBody, String.class);

				handleRateLimitExceed(response);

				Optional<?> responseBody = Optional.ofNullable(response)
						.map(HttpEntity::getBody);
				if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
					String onLabel = getDefaultValueForNullData(detailViewPresentation.getStandbyPowerSwitch().getCommand().getOn(), SmartThingsConstant.ON);
					String offLabel = getDefaultValueForNullData(detailViewPresentation.getStandbyPowerSwitch().getCommand().getOff(), SmartThingsConstant.OFF);

					addAdvanceControlProperties(advancedControllableProperties, createSwitch(stats, convertToTitleCaseIteratingChars(controllableProperty), command, offLabel, onLabel));

					String deviceId = device.getDeviceId();
					retrieveDeviceFullStatus(deviceId);
					Device cachedDevice = new Device(cachedDevices.get(device.getDeviceId()));
					cachedDevicesAfterPollingInterval.put(device.getDeviceId(), cachedDevice);
					populateAggregatedDeviceView(stats, advancedControllableProperties, cachedDevice);
				} else {
					throw new IllegalStateException(String.format("control device %s fail, please try again later", controllableProperty));
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Error while controlling device %s: %s", controllableProperty, e.getMessage()), e);
		}
	}

	/**
	 * This method is used for calling push button control:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param controllableProperty name of controllable property
	 * @param device device data
	 * @param detailViewPresentation device detailViewPresentation
	 * @throws IllegalStateException when fail to control
	 */
	private void pushButtonControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String controllableProperty,
			Device device, DetailViewPresentation detailViewPresentation) {
		try {
			if (device != null) {
				String deviceId = device.getDeviceId();
				String request = SmartThingsURL.DEVICES
						.concat(SmartThingsConstant.SLASH)
						.concat(deviceId)
						.concat(SmartThingsURL.COMMANDS);

				HttpHeaders headers = new HttpHeaders();

				String command = detailViewPresentation.getPushButton().getCommand();
				String requestBody = device.contributeRequestBodyForNonParamCommand(detailViewPresentation.getCapability(), command);
				ResponseEntity<?> response = doRequest(request, HttpMethod.POST, headers, requestBody, String.class);

				handleRateLimitExceed(response);

				Optional<?> responseBody = Optional.ofNullable(response)
						.map(HttpEntity::getBody);
				if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
					addAdvanceControlProperties(advancedControllableProperties,
							createButton(stats, convertToTitleCaseIteratingChars(controllableProperty), SmartThingsConstant.SUCCESSFUL, SmartThingsConstant.SUCCESSFUL));
					retrieveDeviceHealth(deviceId);
					retrieveDevicePresentation(deviceId);
					retrieveDeviceFullStatus(deviceId);
					Device cachedDevice = new Device(cachedDevices.get(device.getDeviceId()));
					cachedDevicesAfterPollingInterval.put(device.getDeviceId(), cachedDevice);
					populateAggregatedDeviceView(stats, advancedControllableProperties, cachedDevice);
				} else {
					throw new IllegalStateException(String.format("control device %s fail, please try again later", controllableProperty));
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Error while controlling device %s: %s", controllableProperty, e.getMessage()), e);
		}
	}

	/**
	 * This method is used for calling slider control:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param value value of controllable property
	 * @param controllableProperty name of controllable property
	 * @param device device data
	 * @param detailViewPresentation device detailViewPresentation
	 * @throws IllegalStateException when fail to control
	 */
	private void sliderControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String value, String controllableProperty,
			Device device, DetailViewPresentation detailViewPresentation) {
		try {
			if (device != null) {
				String deviceId = device.getDeviceId();
				String request = SmartThingsURL.DEVICES
						.concat(SmartThingsConstant.SLASH)
						.concat(deviceId)
						.concat(SmartThingsURL.COMMANDS);

				HttpHeaders headers = new HttpHeaders();

				String command = detailViewPresentation.getSlider().getCommand();
				Integer currentSliderValue = (int) Float.parseFloat(value);

				String requestBody = device.contributeRequestBodyForParameterCommand(detailViewPresentation.getCapability(), command, currentSliderValue.toString());

				ResponseEntity<?> response = doRequest(request, HttpMethod.POST, headers, requestBody, String.class);

				handleRateLimitExceed(response);

				Optional<?> responseBody = Optional.ofNullable(response)
						.map(HttpEntity::getBody);
				if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
					Optional<List<Float>> range = Optional.ofNullable(detailViewPresentation.getSlider())
							.map(Slider::getRange);

					if (range.isPresent() && range.get().size() == 2) {
						retrieveDeviceHealth(deviceId);
						retrieveDevicePresentation(deviceId);
						retrieveDeviceFullStatus(deviceId);
						Device cachedDevice = new Device(cachedDevices.get(device.getDeviceId()));
						cachedDevicesAfterPollingInterval.put(device.getDeviceId(), cachedDevice);
						populateAggregatedDeviceView(stats, advancedControllableProperties, cachedDevice);
					}
				} else {
					throw new IllegalStateException(String.format("control device %s fail, please try again later", controllableProperty));
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Error while controlling device %s: %s", controllableProperty, e.getMessage()), e);
		}
	}

	/**
	 * This method is used for calling dropdownList control:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param value value of controllable property
	 * @param controllableProperty name of controllable property
	 * @param device device data
	 * @param detailViewPresentation device detailViewPresentation
	 * @throws IllegalStateException when fail to control
	 */
	private void listControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String value, String controllableProperty,
			Device device, DetailViewPresentation detailViewPresentation) {
		try {
			if (device != null) {
				String deviceId = device.getDeviceId();
				String request = SmartThingsURL.DEVICES
						.concat(SmartThingsConstant.SLASH)
						.concat(deviceId)
						.concat(SmartThingsURL.COMMANDS);

				HttpHeaders headers = new HttpHeaders();

				String requestBody;
				Optional<String> commandName = Optional.ofNullable(detailViewPresentation.getDropdownList())
						.map(DropdownList::getCommand)
						.map(Command::getName);
				if (commandName.isPresent()) {
					requestBody = device.contributeRequestBodyForParameterCommand(detailViewPresentation.getCapability(), commandName.get(), String.format("\"%s\"", value));
				} else {
					requestBody = device.contributeRequestBodyForNonParamCommand(detailViewPresentation.getCapability(), value);
				}

				ResponseEntity<?> response = doRequest(request, HttpMethod.POST, headers, requestBody, String.class);

				handleRateLimitExceed(response);

				Optional<?> responseBody = Optional.ofNullable(response)
						.map(HttpEntity::getBody);
				if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
					retrieveDeviceHealth(deviceId);
					retrieveDevicePresentation(deviceId);
					retrieveDeviceFullStatus(deviceId);
					Device cachedDevice = new Device(cachedDevices.get(device.getDeviceId()));
					cachedDevicesAfterPollingInterval.put(device.getDeviceId(), cachedDevice);
					populateAggregatedDeviceView(stats, advancedControllableProperties, cachedDevice);
				} else {
					throw new IllegalStateException(String.format("control device %s fail, please try again later", controllableProperty));
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Error while controlling device %s: %s", controllableProperty, e.getMessage()), e);
		}
	}

	/**
	 * This method is used for calling number control:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param value value of controllable property
	 * @param controllableProperty name of controllable property
	 * @param device device data
	 * @param detailViewPresentation device detailViewPresentation
	 * @throws IllegalStateException when fail to control
	 */
	private void numberControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String value, String controllableProperty,
			Device device, DetailViewPresentation detailViewPresentation) {
		try {
			if (device != null) {
				String request = SmartThingsURL.DEVICES
						.concat(SmartThingsConstant.SLASH)
						.concat(device.getDeviceId())
						.concat(SmartThingsURL.COMMANDS);

				HttpHeaders headers = new HttpHeaders();

				Integer currentNumberValue = (int) Float.parseFloat(value);
				String command = detailViewPresentation.getNumberField().getCommand();
				String requestBody = device.contributeRequestBodyForParameterCommand(detailViewPresentation.getCapability(), command, currentNumberValue.toString());

				ResponseEntity<?> response = doRequest(request, HttpMethod.POST, headers, requestBody, String.class);

				handleRateLimitExceed(response);

				Optional<?> responseBody = Optional.ofNullable(response)
						.map(HttpEntity::getBody);
				if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
					addAdvanceControlProperties(advancedControllableProperties, createNumeric(stats, convertToTitleCaseIteratingChars(detailViewPresentation.getLabel()), currentNumberValue.toString()));

					device.getPresentation().getDetailViewPresentations().remove(detailViewPresentation);
					detailViewPresentation.getNumberField().setValue(currentNumberValue.toString());
					device.getPresentation().getDetailViewPresentations().add(detailViewPresentation);
					cachedDevicesAfterPollingInterval.put(device.getDeviceId(), device);
					cachedDevices.put(device.getDeviceId(), device);
				} else {
					throw new IllegalStateException(String.format("control device %s fail, please try again later", controllableProperty));
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Error while controlling device %s: %s", controllableProperty, e.getMessage()), e);
		}
	}

	/**
	 * This method is used for calling textControl control:
	 *
	 * @param stats is the map that store all statistics
	 * @param advancedControllableProperties is the list that store all controllable properties
	 * @param value value of controllable property
	 * @param controllableProperty name of controllable property
	 * @param device device data
	 * @param detailViewPresentation device detailViewPresentation
	 * @throws IllegalStateException when fail to control
	 */
	private void textControl(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String value, String controllableProperty,
			Device device, DetailViewPresentation detailViewPresentation) {
		try {
			if (device != null) {
				String request = SmartThingsURL.DEVICES
						.concat(SmartThingsConstant.SLASH)
						.concat(device.getDeviceId())
						.concat(SmartThingsURL.COMMANDS);

				HttpHeaders headers = new HttpHeaders();

				String command = detailViewPresentation.getTextField().getCommand();
				String requestBody = device.contributeRequestBodyForParameterCommand(detailViewPresentation.getCapability(), command, String.format("\"%s\"", value));

				ResponseEntity<?> response = doRequest(request, HttpMethod.POST, headers, requestBody, String.class);

				handleRateLimitExceed(response);

				Optional<?> responseBody = Optional.ofNullable(response)
						.map(HttpEntity::getBody);
				if (response.getStatusCode().is2xxSuccessful() && responseBody.isPresent()) {
					addAdvanceControlProperties(advancedControllableProperties, createText(stats, convertToTitleCaseIteratingChars(detailViewPresentation.getLabel()), value));

					device.getPresentation().getDetailViewPresentations().remove(detailViewPresentation);
					detailViewPresentation.getTextField().setValue(value);
					device.getPresentation().getDetailViewPresentations().add(detailViewPresentation);
					cachedDevicesAfterPollingInterval.put(device.getDeviceId(), device);
					cachedDevices.put(device.getDeviceId(), device);
				} else {
					throw new IllegalStateException(String.format("control device %s fail, please try again later", controllableProperty));
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Error while controlling device %s: %s", controllableProperty, e.getMessage()), e);
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
		deviceIds.clear();
		Set<String> filteredCategories = new HashSet<>();
		Set<String> filteredNames = new HashSet<>();
		Set<String> filteredRooms = new HashSet<>();

		Set<String> supportedCategories = Arrays.stream(DeviceCategoriesMetric.values())
				.filter(DeviceCategoriesMetric::isImplement)
				.map(categoriesMetric -> categoriesMetric.getName().toUpperCase())
				.collect(Collectors.toSet());

		if (deviceTypeFilter != null) {
			filteredCategories = convertUserInput(deviceTypeFilter.toUpperCase());
		}
		if (deviceNameFilter != null) {
			filteredNames = convertUserInput(deviceNameFilter.toUpperCase());
		}
		if (roomFilter != null) {
			filteredRooms = convertUserInput(roomFilter.toUpperCase());
		}
		if (filteredRooms.contains(SmartThingsConstant.NO_ROOM_ASSIGNED.toUpperCase())) {
			filteredRooms.remove(SmartThingsConstant.NO_ROOM_ASSIGNED.toUpperCase());
			filteredRooms.add(SmartThingsConstant.EMPTY);
		}

		for (Device device : cachedDevices.values()) {
			if (!supportedCategories.contains(device.retrieveCategory().toUpperCase())) {
				continue;
			}

			String deviceCategory = getDefaultValueForNullData(DeviceCategoriesMetric.getUiNameByName(device.retrieveCategory()), SmartThingsConstant.NONE);
			if (deviceCategory.equals(DeviceCategoriesMetric.SWITCH.getName()) && StringUtils.isNotNullOrEmpty(device.getDeviceManufacturerCode())) {
				deviceCategory = SmartThingsConstant.POWER;
			}
			if (!filteredCategories.isEmpty() && !filteredCategories.contains(deviceCategory.toUpperCase())) {
				continue;
			}
			if (!filteredRooms.isEmpty() && !filteredRooms.contains(findRoomNameById(device.getRoomId()).toUpperCase())) {
				continue;
			}
			if (!filteredNames.isEmpty() && !filteredNames.contains(device.getName().toUpperCase())) {
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
	private Set<String> convertUserInput(String input) {
		try {
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
		} catch (Exception e) {
			logger.error("In valid adapter properties input");
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
	 * @param stats extended statistics
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
	 * @param stats extended statistics
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
	 * @param stats extended statistics
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
	 *
	 * @param stats extended statistics
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
	 * Create a controllable property Numeric
	 *
	 * @param stats extended statistics
	 * @param name the name of property
	 * @param initialValue character String
	 * @return AdvancedControllableProperty Text instance
	 */
	private AdvancedControllableProperty createNumeric(Map<String, String> stats, String name, String initialValue) {
		stats.put(name, initialValue);
		AdvancedControllableProperty.Numeric numeric = new AdvancedControllableProperty.Numeric();
		return new AdvancedControllableProperty(name, new Date(), numeric, initialValue);
	}

	/***
	 * Create AdvancedControllableProperty slider instance
	 *
	 * @param stats extended statistics
	 * @param name name of the control
	 * @param initialValue initial value of the control
	 * @return AdvancedControllableProperty slider instance
	 */
	private AdvancedControllableProperty createSlider(Map<String, String> stats, String name, String labelStart, String labelEnd, Float rangeStart, Float rangeEnd, Float initialValue) {
		stats.put(name, initialValue.toString());
		AdvancedControllableProperty.Slider slider = new AdvancedControllableProperty.Slider();
		slider.setLabelStart(labelStart);
		slider.setLabelEnd(labelEnd);
		slider.setRangeStart(rangeStart);
		slider.setRangeEnd(rangeEnd);

		return new AdvancedControllableProperty(name, new Date(), slider, initialValue);
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
	 * @throws IllegalArgumentException when get limit rate exceed error
	 */
	private int calculatingLocalPollingInterval() {

		try {
			int pollingIntervalValue = SmartThingsConstant.MIN_POLLING_INTERVAL;
			if (StringUtils.isNotNullOrEmpty(pollingInterval)) {
				pollingIntervalValue = Integer.parseInt(pollingInterval);
			}

			int minPollingInterval = calculatingMinPollingInterval();
			if (pollingIntervalValue < minPollingInterval) {
				logger.error(String.format("invalid pollingInterval value, pollingInterval must greater than: %s", minPollingInterval));
				return minPollingInterval;
			}
			return pollingIntervalValue;
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Unexpected pollingInterval value: %s", pollingInterval));
		}
	}

	/**
	 * calculating minimum of polling interval
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
	 * calculating thread quantity
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
	 * @throws IllegalStateException when get limit rate exceed error
	 */
	private void handleRateLimitExceed(ResponseEntity<?> response) {
		Optional<String> rateLimit = Optional.ofNullable(response)
				.map(HttpEntity::getHeaders)
				.map(l -> l.get(SmartThingsConstant.RATE_LIMIT_HEADER_KEY))
				.map(t -> t.get(0));
		if (response.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS) && rateLimit.isPresent()) {
			Integer resetTime = Integer.parseInt(rateLimit.get()) / 1000;
			throw new IllegalStateException(String.format("Rate limit exceeded; request rejected. please waiting for %s", resetTime));
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
	 * @return Location
	 */
	private Location findLocationByName(String name) {
		Objects.requireNonNull(cachedLocations);
		if (StringUtils.isNotNullOrEmpty(name)) {
			Location location = cachedLocations.stream().filter(l -> name.equalsIgnoreCase(l.getName())).findFirst().orElse(new Location());
			return location;
		} else {
			return cachedLocations.get(0);
		}
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
	 * Find control by label
	 *
	 * @param label control label
	 * @param detailViewPresentations List of device detail view presentation
	 * @return String sceneId
	 */
	private DetailViewPresentation findControlByLabel(String label, List<DetailViewPresentation> detailViewPresentations) {
		Optional<DetailViewPresentation> detailViewPresentation = detailViewPresentations.stream().filter(d -> label.equals(d.getLabel())).findFirst();
		return detailViewPresentation.orElse(null);
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
	private void isValidConfigManagement() {
		isConfigManagement = StringUtils.isNotNullOrEmpty(this.configManagement) && this.configManagement.equalsIgnoreCase(SmartThingsConstant.IS_VALID_CONFIG_MANAGEMENT);
	}

	/**
	 * This method is used to init Map<colorName, colorCode> of common color
	 */
	private void initCommonColors() {
		commonColors.put(AggregatedDeviceColorControllingConstant.BLUE, Color.BLUE);
		commonColors.put(AggregatedDeviceColorControllingConstant.CYAN, Color.CYAN);
		commonColors.put(AggregatedDeviceColorControllingConstant.GREEN, Color.GREEN);
		commonColors.put(AggregatedDeviceColorControllingConstant.ORANGE, Color.ORANGE);
		commonColors.put(AggregatedDeviceColorControllingConstant.PINK, Color.PINK);
		commonColors.put(AggregatedDeviceColorControllingConstant.RED, Color.RED);
		commonColors.put(AggregatedDeviceColorControllingConstant.WHITE, Color.WHITE);
		commonColors.put(AggregatedDeviceColorControllingConstant.YELLOW, Color.YELLOW);
	}

	/**
	 * This method is used to init Map<colorName, colorCode> of common color
	 */
	private void mapAggregatedDevicesToCache() {
		for (Device device : cachedDevicesAfterPollingInterval.values()) {
			String deviceId = device.getDeviceId();
			if (!deviceIds.contains(deviceId)) {
				cachedAggregatedDevices.remove(deviceId);
				continue;
			}
			AggregatedDevice cachedAggregatedDevice = Optional.ofNullable(cachedAggregatedDevices.get(deviceId)).orElse(new AggregatedDevice());

			cachedAggregatedDevice.setDeviceName(getDefaultValueForNullData(device.getName(), SmartThingsConstant.NONE));
			cachedAggregatedDevice.setDeviceId(device.getDeviceId());

			String deviceCategory = getDefaultValueForNullData(DeviceCategoriesMetric.getUiNameByName(device.retrieveCategory()), SmartThingsConstant.NONE);

			if (deviceCategory.equals(DeviceCategoriesMetric.SWITCH.getName()) && StringUtils.isNotNullOrEmpty(device.getDeviceManufacturerCode())) {
				deviceCategory = SmartThingsConstant.POWER;
			}
			cachedAggregatedDevice.setCategory(deviceCategory);
			cachedAggregatedDevice.setType(SmartThingsConstant.DEFAULT_DEVICE_TYPE);
			cachedAggregatedDevice.setDeviceOnline(convertDeviceStatusValue(device));

			Map<String, String> properties = new HashMap<>();
			List<AdvancedControllableProperty> controllableProperties = new ArrayList<>();

			populateAggregatedDeviceView(properties, controllableProperties, device);

			cachedAggregatedDevice.setProperties(properties);
			cachedAggregatedDevice.setControllableProperties(controllableProperties);

			cachedAggregatedDevices.put(deviceId, cachedAggregatedDevice);
		}
	}

	/**
	 * This method is used to convert text to title case
	 *
	 * @param text input string
	 * @return String title case text
	 */
	private String convertToTitleCaseIteratingChars(String text) {
		if (StringUtils.isNullOrEmpty(text)) {
			return text;
		}
		StringBuilder converted = new StringBuilder();

		boolean convertNext = true;
		for (char ch : text.toCharArray()) {
			if (Character.isSpaceChar(ch)) {
				convertNext = true;
				continue;
			} else if (convertNext) {
				ch = Character.toTitleCase(ch);
				convertNext = false;
			} else {
				ch = Character.toLowerCase(ch);
			}
			converted.append(ch);
		}
		String convertedText = converted.toString();
		controllablePropertyNames.put(convertedText, text);
		return convertedText;
	}
}