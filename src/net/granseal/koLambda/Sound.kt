package net.granseal.koLambda

import java.io.BufferedInputStream
import java.io.InputStream
import javax.sound.sampled.AudioSystem

class Sound(file: BufferedInputStream) {
    val clip = AudioSystem.getClip()
    init {
        val ais = AudioSystem.getAudioInputStream(file)
        clip.open(ais)
        ais.close()
    }

    fun play() {
        if (clip.isRunning || clip.isActive) clip.stop()
        clip.framePosition = 0
        clip.start()
    }
}