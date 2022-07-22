package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
import com.avispl.symphony.dal.communicator.RestCommunicator;

/**
 * com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.SamSungSmartThingsAggregatorCommunicator
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/22/2022
 * @since 1.0.0
 */
public class SamSungSmartThingsAggregatorCommunicator extends RestCommunicator implements Aggregator, Monitorable, Controller {

	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		Map<String, String> stats = new HashMap<>();
		stats.put("DeviceOnline", "true");
		extendedStatistics.setStatistics(stats);

		return Collections.singletonList(extendedStatistics);
	}

	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {

	}

	@Override
	public void controlProperties(List<ControllableProperty> list) throws Exception {

	}

	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
		return null;
	}

	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics(List<String> list) throws Exception {
		return null;
	}

	@Override
	protected void authenticate() throws Exception {

	}
}
