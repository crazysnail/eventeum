/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.consensys.eventeum.service.catchup;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.consensys.eventeum.chain.contract.ContractEventListener;
import net.consensys.eventeum.chain.service.BlockchainService;
import net.consensys.eventeum.chain.service.block.BlockStartNumberService;
import net.consensys.eventeum.chain.service.container.ChainServicesContainer;
import net.consensys.eventeum.dto.event.ContractEventDetails;
import net.consensys.eventeum.dto.event.filter.ContractEventFilter;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class DefaultEventCatchupService implements EventCatchupService {

    private List<ContractEventListener> contractEventListeners;

    private ChainServicesContainer servicesContainer;

    private BlockStartNumberService blockStartNumberService;

    private EventRetriever eventRetriever;

    @Override
    public void catchup(List<ContractEventFilter> filters) {

        filters.forEach(filter -> {
            final BlockchainService blockchainService = servicesContainer
                    .getNodeServices(filter.getNode())
                    .getBlockchainService();

            eventRetriever.retrieveEvents(filter,filter.getStartBlock(), blockchainService.getCurrentBlockNumber())
                    .forEach(contractEvent -> triggerListeners(contractEvent));

        });

    }

    private void triggerListeners(ContractEventDetails contractEvent) {
        contractEventListeners.forEach(
                listener -> triggerListener(listener, contractEvent));
    }

    private void triggerListener(ContractEventListener listener, ContractEventDetails contractEventDetails) {
        try {
            listener.onEvent(contractEventDetails);
        } catch (Throwable t) {
            log.error(String.format(
                    "An error occurred when processing contractEvent with id %s", contractEventDetails.getId()), t);
        }
    }
}