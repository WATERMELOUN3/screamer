package powerdancer.dsp.filter.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import powerdancer.dsp.event.Event
import powerdancer.dsp.filter.Filter

class Forker(private vararg val pipelines: Array<Filter>): Filter {
    val logger: Logger = LoggerFactory.getLogger(Forker::class.java)

    @OptIn(FlowPreview::class)
    override suspend fun filter(event: Event): Flow<Event> {
        val supervisor = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Default + supervisor)

        return flow {
            pipelines.forEach { pipeline->
                scope.launch {
                    val eventCopy = event.clone()
                    pipeline
                        .fold(
                            flowOf(eventCopy.first)
                        ) { accumulatedFlow: Flow<Event>, filter: Filter ->
                            accumulatedFlow.flatMapConcat { event ->
                                filter.filter(event)
                            }
                        }.onCompletion {
                            eventCopy.second()
                        }.collect()
                }
            }
            supervisor.complete()
            supervisor.join()
        }


    }

}