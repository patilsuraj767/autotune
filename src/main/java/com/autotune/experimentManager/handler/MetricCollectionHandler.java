/*******************************************************************************
 * Copyright (c)  2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.experimentManager.handler;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.experimentManager.data.result.CycleMetaData;
import com.autotune.experimentManager.data.result.StepsMetaData;
import com.autotune.experimentManager.data.result.TrialIterationMetaData;
import com.autotune.experimentManager.handler.eminterface.EMHandlerInterface;
import com.autotune.experimentManager.handler.util.EMStatusUpdateHandler;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.stream.IntStream;

/**
 * Metric collection helper class.
 */
public class MetricCollectionHandler implements EMHandlerInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricCollectionHandler.class);

    @Override
    public void execute(ExperimentTrial experimentTrial, TrialDetails trialDetails, TrialIterationMetaData iterationMetaData, StepsMetaData stepsMeatData, AutotuneExecutor autotuneExecutor, ServletContext context) {
        try {
            LOGGER.debug("ExperimentName: \"{}\" - TrialNo: {} - Iteration: {} - StepName: {}",
                    experimentTrial.getExperimentName(),
                    trialDetails.getTrialNumber(),
                    iterationMetaData.getIterationNumber(),
                    stepsMeatData.getStepName()
            );
            stepsMeatData.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            stepsMeatData.setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
            /**
             * Implement MetricCollectionHandler Logic
             */
            LinkedHashMap<String, LinkedHashMap<Integer, CycleMetaData>> cycleMetaDataMap = new LinkedHashMap<>();
            LinkedHashMap<String, Integer> cycles = new LinkedHashMap<>();
            String warmupCycles = experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupCycles();
            String measurementCycles = experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementCycles();
            int warmupCyclesCount = (warmupCycles != null) ? Integer.parseInt(warmupCycles) : -1;
            int measurementCyclesCount = (measurementCycles != null) ? Integer.parseInt(measurementCycles) : -1;
            if (warmupCyclesCount > 0) cycles.put("WarmupCycles", warmupCyclesCount);
            if (measurementCyclesCount > 0) cycles.put("MeasurementCycles", measurementCyclesCount);
            cycles.forEach((cycleName, count) -> {
                LinkedHashMap<Integer, CycleMetaData> iterationCycle = new LinkedHashMap<>();
                IntStream.rangeClosed(1, count).forEach((iteration) -> {
                    CycleMetaData cycleMetaData = new CycleMetaData();
                    cycleMetaData.setCycleName(cycleName);
                    cycleMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                    iterationCycle.put(iteration, cycleMetaData);
                });
                cycleMetaDataMap.put(cycleName, iterationCycle);
            });
            stepsMeatData.setEndTimestamp(new Timestamp(System.currentTimeMillis()));
            stepsMeatData.setStatus(EMUtil.EMExpStatus.COMPLETED);
            EMStatusUpdateHandler.updateTrialIterationDataStatus(experimentTrial, trialDetails, iterationMetaData);
            EMStatusUpdateHandler.updateTrialMetaDataStatus(experimentTrial, trialDetails);
            EMStatusUpdateHandler.updateExperimentTrialMetaDataStatus(experimentTrial);
            autotuneExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            AutotuneWorker theWorker = new CallableFactory().create(autotuneExecutor.getWorker());
                            theWorker.execute(experimentTrial, autotuneExecutor, context);
                        }
                    }
            );
        } catch (Exception e) {
            trialDetails.getTrialMetaData().setStatus(EMUtil.EMExpStatus.FAILED);
            e.printStackTrace();
            LOGGER.error("Failed to execute MetricCollectionHandler ExperimentName: \"{}\" - TrialNo: {} - Iteration: {} - StepName: {} -- due to {}",
                    experimentTrial.getExperimentName(),
                    trialDetails.getTrialNumber(),
                    iterationMetaData.getIterationNumber(),
                    stepsMeatData.getStepName(),
                    e.getMessage()
            );
        }
    }
}