package powerdancer.dsp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import powerdancer.dsp.event.Bump
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.Init
import powerdancer.dsp.filter.Filter

object Processor {
    val logger: Logger = LoggerFactory.getLogger(Processor::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + CoroutineName("powerdancer.dsp.Processor"))

    @OptIn(FlowPreview::class)
    fun process(vararg filters: Filter) = scope.launch {
        filters.asFlow().fold(
            flow {
                emit(Init)
                while(true) {
                    emit(Bump)
                }
            }
        ) { accumulatedFlow: Flow<Event>, filter: Filter ->
            accumulatedFlow.flatMapConcat { event->
                filter.filter(event)
            }
        }.collect()

    }

}