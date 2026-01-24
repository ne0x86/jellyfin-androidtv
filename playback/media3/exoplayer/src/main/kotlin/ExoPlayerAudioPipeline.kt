package org.jellyfin.playback.media3.exoplayer

import android.media.audiofx.LoudnessEnhancer
import timber.log.Timber

class ExoPlayerAudioPipeline {
	private var loudnessEnhancer: LoudnessEnhancer? = null
	private var audioSessionId: Int? = null

	var normalizationGain: Float? = null
		set(value) {
			Timber.d("Normalization gain changed to $value")
			field = value
			applyGain()
		}

	fun setAudioSessionId(audioSessionId: Int) {
		Timber.d("Audio session id changed to $audioSessionId")
		this.audioSessionId = audioSessionId

		// Release any existing enhancer
		loudnessEnhancer?.release()
		loudnessEnhancer = null
		
		// Apply gain if we have a value and a session
		if (normalizationGain != null) {
			createLoudnessEnhancer(audioSessionId)
			applyGain()
		}
	}

	private fun createLoudnessEnhancer(audioSessionId: Int) {
		loudnessEnhancer = runCatching { LoudnessEnhancer(audioSessionId) }
			.onFailure { Timber.w(it, "Failed to create LoudnessEnhancer") }
			.getOrNull()
	}

	private fun applyGain() {
		val gain = normalizationGain
		if (gain == null) {
			loudnessEnhancer?.release()
			loudnessEnhancer = null
			return
		}

		if (loudnessEnhancer == null) {
			val sessionId = audioSessionId
			if (sessionId != null) {
				createLoudnessEnhancer(sessionId)
			}
		}

		if (loudnessEnhancer == null) {
			Timber.d("LoudnessEnhancer not initialized yet (missing sessionId or creation failed)")
			return
		}

		val targetGain = gain
			.times(100f)
			.toInt()

		Timber.d("Applying gain (targetGain=$targetGain)")
		loudnessEnhancer?.enabled = true
		loudnessEnhancer?.setTargetGain(targetGain)
	}
}
