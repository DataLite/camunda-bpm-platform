/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm.engine.impl.bpmn.behavior;

import java.util.List;

import org.camunda.bpm.engine.impl.bpmn.helper.CompensationUtil;
import org.camunda.bpm.engine.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.PvmActivity;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.pvm.runtime.LegacyBehavior;
import org.camunda.bpm.engine.impl.util.EnsureUtil;


/**
 * @author Daniel Meyer
 * @author Falko Menge
 */
public class CancelEndEventActivityBehavior extends AbstractBpmnActivityBehavior {

  protected PvmActivity cancelBoundaryEvent;

  @Override
  public void execute(ActivityExecution execution) throws Exception {

    EnsureUtil
    .ensureNotNull("Could not find cancel boundary event for cancel end event " + execution.getActivity(), "cancelBoundaryEvent", cancelBoundaryEvent);

    ExecutionEntity executionEntity = (ExecutionEntity) execution;
    List<CompensateEventSubscriptionEntity> compensateEventSubscriptions = executionEntity.getCompensateEventSubscriptions();
    if(compensateEventSubscriptions.isEmpty()) {
      leave(execution);
    }
    else {
      CompensationUtil.throwCompensationEvent(compensateEventSubscriptions, execution, false);
    }

  }

  protected void leave(ActivityExecution execution) {
    // continue via the appropriate cancel boundary event
    ScopeImpl eventScope = (ScopeImpl) cancelBoundaryEvent.getEventScope();
    eventScope = LegacyBehavior.get().normalizeSecondNonScope(eventScope);

    ActivityExecution boundaryEventScopeExecution = execution.findExecutionForFlowScope(eventScope);
    boundaryEventScopeExecution.executeActivity(cancelBoundaryEvent);
  }

  public void signal(ActivityExecution execution, String signalName, Object signalData) throws Exception {

    // join compensating executions
    if(execution.getExecutions().isEmpty()) {
      leave(execution);
    } else {
      ((ExecutionEntity)execution).forceUpdate();
    }
  }

  public void setCancelBoundaryEvent(PvmActivity cancelBoundaryEvent) {
    this.cancelBoundaryEvent = cancelBoundaryEvent;
  }

  public PvmActivity getCancelBoundaryEvent() {
    return cancelBoundaryEvent;
  }

}
