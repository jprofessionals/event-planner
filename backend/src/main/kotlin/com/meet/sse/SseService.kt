package com.meet.sse

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class SseEvent(
    val type: String,
    val data: Any,
)

@ApplicationScoped
class SseService {
    private val emitters = ConcurrentHashMap<UUID, CopyOnWriteArrayList<MultiEmitter<in SseEvent>>>()

    fun subscribe(eventId: UUID): Multi<SseEvent> =
        Multi.createFrom().emitter { emitter ->
            val list = emitters.computeIfAbsent(eventId) { CopyOnWriteArrayList() }
            list.add(emitter)

            emitter.onTermination {
                emitters.compute(eventId) { _, current ->
                    if (current == null) return@compute null
                    current.remove(emitter)
                    if (current.isEmpty()) null else current
                }
            }
        }

    fun broadcast(
        eventId: UUID,
        type: String,
        data: Any,
    ) {
        val eventEmitters = emitters[eventId] ?: return
        val event = SseEvent(type = type, data = data)
        eventEmitters.forEach { emitter ->
            try {
                emitter.emit(event)
            } catch (_: Exception) {
                // Emitter may be closed
            }
        }
    }
}
