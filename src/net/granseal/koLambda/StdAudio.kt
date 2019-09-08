package net.granseal.koLambda

/******************************************************************************
 *  Compilation:  javac StdAudio.java
 *  Execution:    java StdAudio
 *  Dependencies: none
 *
 *  Simple library for reading, writing, and manipulating .wav files.
 *
 *
 *  Limitations
 *  -----------
 *    - Assumes the audio is monaural, little endian, with sampling rate
 *      of 44,100
 *    - check when reading .wav files from a .jar file ?
 *
 ******************************************************************************/

import javax.sound.sampled.Clip

import java.io.File
import java.io.ByteArrayInputStream
import java.io.IOException

import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.UnsupportedAudioFileException

import kotlin.experimental.*
import kotlin.math.pow
import kotlin.math.sin

/**
 * *Standard audio*. This class provides a basic capability for
 * creating, reading, and saving audio.
 *
 *
 * The audio format uses a sampling rate of 44,100 Hz, 16-bit, monaural.
 *
 *
 *
 * For additional documentation, see [Section 1.5](https://introcs.cs.princeton.edu/15inout) of
 * *Computer Science: An Interdisciplinary Approach* by Robert Sedgewick and Kevin Wayne.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 */
object StdAudio {

    /**
     * The sample rate: 44,100 Hz for CD quality audio.
     */
    const val SAMPLE_RATE = 44100

    private const val BYTES_PER_SAMPLE = 2       // 16-bit audio
    private const val BITS_PER_SAMPLE = 16       // 16-bit audio
    private const val MAX_16_BIT = 32768.0
    private const val SAMPLE_BUFFER_SIZE = 4096

    private const val MONO = 1
    private const val STEREO = 2
    private const val LITTLE_ENDIAN = false
    private const val BIG_ENDIAN = true
    private const val SIGNED = true
    private const val UNSIGNED = false

    private var line: SourceDataLine? = null   // to play the sound
    private var buffer: ByteArray? = null         // our internal buffer
    private var bufferSize = 0    // number of samples currently in internal buffer

    // static initializer
    init {
        // open up an audio stream
        try {
            // 44,100 Hz, 16-bit audio, mono, signed PCM, little endian
            val format = AudioFormat(SAMPLE_RATE.toFloat(), BITS_PER_SAMPLE, MONO, SIGNED, LITTLE_ENDIAN)
            val info = DataLine.Info(SourceDataLine::class.java, format)

            line = AudioSystem.getLine(info) as SourceDataLine
            line!!.open(format, SAMPLE_BUFFER_SIZE * BYTES_PER_SAMPLE)

            // the internal buffer is a fraction of the actual buffer size, this choice is arbitrary
            // it gets divided because we can't expect the buffered data to line up exactly with when
            // the sound card decides to push out its samples.
            buffer = ByteArray(SAMPLE_BUFFER_SIZE * BYTES_PER_SAMPLE / 3)
        } catch (e: LineUnavailableException) {
            println(e.message)
        }

        // no sound gets made before this call
        line!!.start()
    }

    // get an AudioInputStream object from a file
    private fun getAudioInputStreamFromFile(filename: String?): AudioInputStream {
        requireNotNull(filename) { "filename is null" }

        try {
            // first try to read file from local file system
            val file = File(filename)
            if (file.exists()) {
                return AudioSystem.getAudioInputStream(file)
            }

            // resource relative to .class file
            val is1 = StdAudio::class.java.getResourceAsStream(filename)
            if (is1 != null) {
                return AudioSystem.getAudioInputStream(is1)
            }

            // resource relative to classloader root
            val is2 = StdAudio::class.java.classLoader.getResourceAsStream(filename)
            return if (is2 != null) {
                AudioSystem.getAudioInputStream(is2)
            } else {
                throw IllegalArgumentException("could not read '$filename'")
            }// give up
        } catch (e: IOException) {
            throw IllegalArgumentException("could not read '$filename'", e)
        } catch (e: UnsupportedAudioFileException) {
            throw IllegalArgumentException("file of unsupported audio format: '$filename'", e)
        }

    }

    /**
     * Closes standard audio.
     */
    fun close() {
        line!!.drain()
        line!!.stop()
    }

    /**
     * Writes one sample (between -1.0 and +1.0) to standard audio.
     * If the sample is outside the range, it will be clipped.
     *
     * @param  s the sample to play
     * @throws IllegalArgumentException if the sample is `Double.NaN`
     */
    fun play(s: Double) {
        var sample = s
        require(!java.lang.Double.isNaN(sample)) { "sample is NaN" }

        // clip if outside [-1, +1]
        if (sample < -1.0) sample = -1.0
        if (sample > +1.0) sample = +1.0

        // convert to bytes
        var s = (MAX_16_BIT * sample).toShort()
        if (sample == 1.0) s = java.lang.Short.MAX_VALUE   // special case since 32768 not a short
        buffer?.set(bufferSize++, s.toByte())
        buffer?.set(bufferSize++, (s.toInt() shr 8).toByte())   // little endian

        // send to sound card if buffer is full
        if (bufferSize >= buffer!!.size) {
            line!!.write(buffer, 0, buffer!!.size)
            bufferSize = 0
        }
    }

    /**
     * Writes the array of samples (between -1.0 and +1.0) to standard audio.
     * If a sample is outside the range, it will be clipped.
     *
     * @param  samples the array of samples to play
     * @throws IllegalArgumentException if any sample is `Double.NaN`
     * @throws IllegalArgumentException if `samples` is `null`
     */
    fun play(samples: DoubleArray?) {
        requireNotNull(samples) { "argument to play() is null" }
        for (i in samples.indices) {
            play(samples[i])
        }
    }

    /**
     * Reads audio samples from a file (in .wav or .au format) and returns
     * them as a double array with values between -1.0 and +1.0.
     * The audio file must be 16-bit with a sampling rate of 44,100.
     * It can be mono or stereo.
     *
     * @param  filename the name of the audio file
     * @return the array of samples
     */
    fun read(filename: String): DoubleArray {

        // make sure that AudioFormat is 16-bit, 44,100 Hz, little endian
        val ais = getAudioInputStreamFromFile(filename)
        val audioFormat = ais.format

        // require sampling rate = 44,100 Hz
        require(audioFormat.sampleRate == SAMPLE_RATE.toFloat()) { ("StdAudio.read() currently supports only a sample rate of " + SAMPLE_RATE + " Hz\n"
            + "audio format: " + audioFormat) }

        // require 16-bit audio
        require(audioFormat.sampleSizeInBits == BITS_PER_SAMPLE) { ("StdAudio.read() currently supports only " + BITS_PER_SAMPLE + "-bit audio\n"
            + "audio format: " + audioFormat) }

        // require little endian
        require(!audioFormat.isBigEndian) { ("StdAudio.read() currently supports only audio stored using little endian\n"
            + "audio format: " + audioFormat) }

        var bytes: ByteArray? = null
        try {
            val bytesToRead = ais.available()
            bytes = ByteArray(bytesToRead)
            val bytesRead = ais.read(bytes)
            check(bytesToRead == bytesRead) { "read only $bytesRead of $bytesToRead bytes" }
        } catch (ioe: IOException) {
            throw IllegalArgumentException("could not read '$filename'", ioe)
        }

        val n = bytes.size

        // little endian, mono
        when {
            audioFormat.channels == MONO -> {
                val data = DoubleArray(n / 2)
                for (i in 0 until n / 2) {
                    // little endian, mono
                    data[i] =
                        ((((bytes[2 * i + 1] and 0xFF.toByte()).toInt() shl 8).toByte() or (bytes[2 * i] and 0xFF.toByte())).toShort()) / (MAX_16_BIT)
                }
                return data
            }
            audioFormat.channels == STEREO -> {
                val data = DoubleArray(n / 4)
                for (i in 0 until n / 4) {
                    val left =
                        ((((bytes[4 * i + 1] and 0xFF.toByte()).toInt() shl 8).toByte() or (bytes[4 * i + 0] and 0xFF.toByte())).toShort()) / (MAX_16_BIT)
                    val right =
                        ((((bytes[4 * i + 3] and 0xFF.toByte()).toInt() shl 8).toByte() or (bytes[4 * i + 2] and 0xFF.toByte())).toShort()) / (MAX_16_BIT)
                    data[i] = (left + right) / 2.0
                }
                return data
            }
            else -> throw IllegalStateException("audio format is neither mono or stereo")
        }// TODO: handle big endian (or other formats)
        // little endian, stereo
    }

    /**
     * Saves the double array as an audio file (using .wav or .au format).
     *
     * @param  filename the name of the audio file
     * @param  samples the array of samples
     * @throws IllegalArgumentException if unable to save `filename`
     * @throws IllegalArgumentException if `samples` is `null`
     * @throws IllegalArgumentException if `filename` is `null`
     * @throws IllegalArgumentException if `filename` extension is not `.wav`
     * or `.au`
     */
    fun save(filename: String?, samples: DoubleArray?) {
        requireNotNull(filename) { "filename is null" }
        requireNotNull(samples) { "samples[] is null" }

        // assumes 16-bit samples with sample rate = 44,100 Hz
        // use 16-bit audio, mono, signed PCM, little Endian
        val format = AudioFormat(SAMPLE_RATE.toFloat(), 16, MONO, SIGNED, LITTLE_ENDIAN)
        val data = ByteArray(2 * samples.size)
        for (i in samples.indices) {
            var temp = (samples[i] * MAX_16_BIT).toShort().toInt()
            if (samples[i] == 1.0) temp = Short.MAX_VALUE.toInt()   // special case since 32768 not a short
            data[2 * i + 0] = temp.toByte()
            data[2 * i + 1] = (temp shr 8).toByte()   // little endian
        }

        // now save the file
        try {
            val bais = ByteArrayInputStream(data)
            val ais = AudioInputStream(bais, format, samples.size.toLong())
            if (filename.endsWith(".wav") || filename.endsWith(".WAV")) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, File(filename))
            } else if (filename.endsWith(".au") || filename.endsWith(".AU")) {
                AudioSystem.write(ais, AudioFileFormat.Type.AU, File(filename))
            } else {
                throw IllegalArgumentException("file type for saving must be .wav or .au")
            }
        } catch (ioe: IOException) {
            throw IllegalArgumentException("unable to save file '$filename'", ioe)
        }

    }


    /**
     * Plays an audio file (in .wav, .mid, or .au format) in a background thread.
     *
     * @param filename the name of the audio file
     * @throws IllegalArgumentException if unable to play `filename`
     * @throws IllegalArgumentException if `filename` is `null`
     */
    @Synchronized
    fun play(filename: String) {
        Thread(Runnable {
            val ais = getAudioInputStreamFromFile(filename)
            stream(ais)
        }).start()
    }


    // https://www3.ntu.edu.sg/home/ehchua/programming/java/J8c_PlayingSound.html
    // play a wav or aif file
    // javax.sound.sampled.Clip fails for long clips (on some systems), perhaps because
    // JVM closes (see remedy in loop)
    private fun stream(ais: AudioInputStream) {
        var line: SourceDataLine? = null
        val bufferSize = 4096 // 4K buffer

        try {
            val audioFormat = ais.format
            val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
            line = AudioSystem.getLine(info) as SourceDataLine
            line.open(audioFormat)
            line.start()
            val samples = ByteArray(bufferSize)
            var count = 0
            while (count != -1) {
                count = ais.read(samples, 0, bufferSize)
                line.write(samples, 0, count)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: LineUnavailableException) {
            e.printStackTrace()
        } finally {
            if (line != null) {
                line.drain()
                line.close()
            }
        }
    }

    /**
     * Loops an audio file (in .wav, .mid, or .au format) in a background thread.
     *
     * @param filename the name of the audio file
     * @throws IllegalArgumentException if `filename` is `null`
     */
    @Synchronized
    fun loop(filename: String?) {
        requireNotNull(filename)

        val ais = getAudioInputStreamFromFile(filename)

        try {
            val clip = AudioSystem.getClip()
            // Clip clip = (Clip) AudioSystem.getLine(new Line.Info(Clip.class));
            clip.open(ais)
            clip.loop(Clip.LOOP_CONTINUOUSLY)
        } catch (e: LineUnavailableException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // keep JVM open
        Thread(Runnable {
            while (true) {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }).start()
    }


    /***************************************************************************
     * Unit tests `StdAudio`.
     */

    // create a note (sine wave) of the given frequency (Hz), for the given
    // duration (seconds) scaled to the given volume (amplitude)
    fun note(hz: Double, duration: Double, amplitude: Double): DoubleArray {
        val n = (SAMPLE_RATE * duration).toInt()
        val a = DoubleArray(n + 1)
        for (i in 0..n)
            a[i] = amplitude * sin(2.0 * Math.PI * i.toDouble() * hz / SAMPLE_RATE)
        return a
    }

    /**
     * Test client - play an A major scale to standard audio.
     *
     * @param args the command-line arguments
     */
    /**
     * Test client - play an A major scale to standard audio.
     *
     * @param args the command-line arguments
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // 440 Hz for 1 sec
        val freq = 440.0
        for (i in 0..SAMPLE_RATE) {
            play(0.15 * sin(2.0 * Math.PI * freq * i.toDouble() / SAMPLE_RATE))
        }

        // scale increments
        val steps = intArrayOf(0, 2, 4, 5, 7, 9, 11, 12)
        for (i in steps.indices) {
            val hz = 440.0 * 2.0.pow(steps[i] / 12.0)
            play(note(hz, 1.0, 0.15))
        }


        // need to call this in non-interactive stuff so the program doesn't terminate
        // until all the sound leaves the speaker.
        close()
    }
}// can not instantiate
