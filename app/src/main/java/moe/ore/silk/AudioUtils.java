package moe.ore.silk;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class AudioUtils {
    public static String tmpDir;

    /**
     * init silk codec
     */
    public static boolean init(String tmpDir) {
        try {
            if (tmpDir == null) {
                return false;
            }
            AudioUtils.tmpDir = tmpDir;
            return "hello".equals(check());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static native String check();

    public static File mp3ToSilk(File mp3File, int bitRate) throws IOException {
        if (!mp3File.exists() || mp3File.length() == 0) {
            return null;
        }
        File pcmFile = getTempFile("pcm");
        File silkFile = getTempFile("silk");
//        int sampleRate = LameCoder.decode(mp3File.getAbsolutePath(), pcmFile.getAbsolutePath());
        FFmpegSession session = FFmpegKit.execute("-y -i " +
                mp3File.getAbsolutePath() +
                " -acodec pcm_s16le -f s16le -ac 1 -ar " +
                "16000" +
                " " +
                pcmFile.getAbsolutePath());
        if (ReturnCode.isSuccess(session.getReturnCode())) {
            SilkCodec.nativeEncodeSilk(pcmFile.getAbsolutePath(), silkFile.getAbsolutePath(), bitRate);
        } else {
            throw new IOException("ffmpeg error");
        }
//        FFmpegKit.execute("-y -f s16be -ac 2 -ar " + bitRate + " -acodec pcm_s16le -i " + pcmFile.getAbsolutePath() + " " + mp3File.getAbsolutePath());
//        pcmFile.delete();
        return silkFile;
    }

    public static File mp3ToSilk(File mp3File) throws IOException {
        return mp3ToSilk(mp3File, SilkCodec.DEFAULT_RATE);
    }

    public static File mp3ToSilk(InputStream mp3FileStream, int bitRate) throws IOException {
        File mp3File = getTempFile("mp3");
        streamToTempFile(mp3FileStream, mp3File);
        return mp3ToSilk(mp3File, bitRate);
    }

    public static File mp3ToSilk(InputStream mp3FileStream) throws IOException {
        return mp3ToSilk(mp3FileStream, 24000);
    }

    public static File silkToMp3(File silkFile, int bitrate) throws IOException {
        if (!silkFile.exists() || silkFile.length() == 0) {
            throw new IOException("文件不存在或为空");
        }
        File pcmFile = getTempFile("pcm");
        File mp3File = getTempFile("mp3");
        SilkCodec.nativeDecodeSilk(silkFile.getAbsolutePath(), pcmFile.getAbsolutePath(), SilkCodec.DEFAULT_RATE);
        LameCoder.encode(pcmFile.getAbsolutePath(), mp3File.getAbsolutePath(), bitrate);
        FFmpegSession session = FFmpegKit.execute("-y -f s16be -ac 1 -ar " + bitrate + " -acodec pcm_s16le -i " + pcmFile.getAbsolutePath() + " " + mp3File.getAbsolutePath());
        if (ReturnCode.isSuccess(session.getReturnCode())) {

        } else {
            throw new IOException("ffmpeg error");
        }
//        pcmFile.delete();
        return mp3File;
    }

    public static File silkToMp3(File silkFile) throws IOException {
        return silkToMp3(silkFile, 24000);
    }

    public static File silkToMp3(InputStream silkFileStream, int bitrate) throws IOException {
        File mp3File = getTempFile("silk");
        streamToTempFile(silkFileStream, mp3File);
        return silkToMp3(mp3File, bitrate);
    }

    public static File silkToMp3(InputStream silkFileStream) throws IOException {
        return silkToMp3(silkFileStream, 24000);
    }

    static void streamToTempFile(InputStream inputStream, File tmpFile) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
        byte[] buf = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buf)) > 0) {
            fileOutputStream.write(buf, 0, bytesRead);
        }
        inputStream.close();
        fileOutputStream.close();
    }


    static File getTempFile(String type) {
        return new File(tmpDir, "tmp_audio_" + type + "_" + UUID.randomUUID() + "." + type);
    }
}
