package moe.ore.silk;

public class SilkCodec {
    public static final int DEFAULT_RATE = 24000;

    public static native int nativeEncodeSilk(String input, String output, int rate);

    public static native int nativeDecodeSilk(String input, String output, int rate);

    /*
    public static void main(String[] args) {
        System.out.println("Silk Codec test....");

        boolean isSuccess = AudioUtils.init("C:\\Users\\13723\\Desktop\\Ore\\library\\libsilkcodec.dll", "C:\\Users\\13723\\Desktop\\Ore\\library\\");

        System.out.println("library is loaded successfully:"+ isSuccess);

        System.out.println("library name:" + AudioUtils.check());

        String input = "C:\\Users\\13723\\Desktop\\Ore\\test.mp3";

        try {
            System.out.println("Encode Result:" + AudioUtils.mp3ToSilk(new File(input)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/
}
