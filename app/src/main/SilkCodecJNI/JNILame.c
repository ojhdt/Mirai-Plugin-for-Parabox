#include "JNILame.h"
#include <string.h>

#define BUFFER_SIZE 8192
#define be_short(s) ((short) ((unsigned short) (s) << 8) | ((unsigned short) (s) >> 8))

lame_global_flags *glf;
hip_t hip;

int read_samples(FILE *input_file, short *input) {
    int nb_read;
    nb_read = fread(input, 1, sizeof(short), input_file) / sizeof(short);

//    int i = 0;
//    while (i < nb_read) {
//        input[i] = be_short(input[i]);
//        i++;
//    }

    return nb_read;
}

JNIEXPORT void JNICALL Java_moe_ore_silk_LameCoder_initializeDefault(
        JNIEnv *env, jclass cls) {

    glf = initializeDefault(env);
}

JNIEXPORT void JNICALL Java_moe_ore_silk_LameCoder_initialize(
        JNIEnv *env, jclass cls, jint inSamplerate, jint outChannel,
        jint outSamplerate, jint outBitrate, jfloat scaleInput, jint mode, jint vbrMode,
        jint quality, jint vbrQuality, jint abrMeanBitrate, jint lowpassFreq, jint highpassFreq,
        jstring id3tagTitle, jstring id3tagArtist, jstring id3tagAlbum,
        jstring id3tagYear, jstring id3tagComment) {

    glf = initialize(env, inSamplerate, outChannel, outSamplerate, outBitrate, scaleInput, mode,
                     vbrMode,
                     quality, vbrQuality, abrMeanBitrate, lowpassFreq, highpassFreq, id3tagTitle,
                     id3tagArtist, id3tagAlbum,
                     id3tagYear,
                     id3tagComment);
}

JNIEXPORT void JNICALL Java_moe_ore_silk_LameCoder_lameClose(
        JNIEnv *env, jclass cls) {
    close(glf);
}

JNIEXPORT void JNICALL Java_moe_ore_silk_LameCoder_encodeFile
        (JNIEnv *env,
         jclass cls, jstring in_source_path, jstring in_target_path) {

    const char *source_path, *target_path;
    source_path = (*env)->GetStringUTFChars(env, in_source_path, NULL);
    target_path = (*env)->GetStringUTFChars(env, in_target_path, NULL);

    FILE *input_file, *output_file;
    input_file = fopen(source_path, "rb");
    output_file = fopen(target_path, "wb");

    short input[BUFFER_SIZE];
    unsigned char output[BUFFER_SIZE];
    int nb_read = 0;
    int nb_write = 0;
    int nb_total = 0;

    nb_read = read_samples(input_file, input);
    while (nb_read) {
        nb_write = lame_encode_buffer(glf, input, input, nb_read, output,
                                      BUFFER_SIZE);
        fwrite(output, nb_write, 1, output_file);
        nb_total += nb_write;
        nb_read = read_samples(input_file, input);
    }

    nb_write = lame_encode_flush(glf, output, BUFFER_SIZE);
    fwrite(output, nb_write, 1, output_file);

    fclose(input_file);
    fclose(output_file);
}


lame_global_flags* initializeDefault(JNIEnv *env) {
    lame_global_flags *glf = lame_init();
    lame_init_params(glf);
    return glf;
}

lame_global_flags *initialize(
        JNIEnv *env,
        jint inSamplerate, jint outChannel,
        jint outSamplerate, jint outBitrate, jfloat scaleInput, jint mode, jint vbrMode,
        jint quality, jint vbrQuality, jint abrMeanBitrate, jint lowpassFreq, jint highpassFreq,
        jstring id3tagTitle, jstring id3tagArtist, jstring id3tagAlbum,
        jstring id3tagYear, jstring id3tagComment) {

    lame_global_flags *glf = lame_init();
    lame_set_in_samplerate(glf, inSamplerate);
    lame_set_num_channels(glf, outChannel);
    lame_set_out_samplerate(glf, outSamplerate);
    lame_set_brate(glf, outBitrate);
    lame_set_quality(glf, quality);
    lame_set_scale(glf, scaleInput);
    lame_set_VBR_q(glf, vbrQuality);
    lame_set_VBR_mean_bitrate_kbps(glf, abrMeanBitrate);
    lame_set_lowpassfreq(glf, lowpassFreq);
    lame_set_highpassfreq(glf, highpassFreq);

    switch (mode) {
        case 0:
            lame_set_mode(glf, STEREO);
            break;
        case 1:
            lame_set_mode(glf, JOINT_STEREO);
            break;
        case 3:
            lame_set_mode(glf, MONO);
            break;
        case 4:
            lame_set_mode(glf, NOT_SET);
            break;
    }

    switch (vbrMode) {
        case 0:
            lame_set_VBR(glf, vbr_off);
            break;
        case 2:
            lame_set_VBR(glf, vbr_rh);
            break;
        case 3:
            lame_set_VBR(glf, vbr_abr);
            break;
        case 4:
            lame_set_VBR(glf, vbr_mtrh);
            break;
        case 6:
            lame_set_VBR(glf, vbr_default);
            break;
        default:
            lame_set_VBR(glf, vbr_off);
            break;

    }


    const jchar *title = NULL;
    const jchar *artist = NULL;
    const jchar *album = NULL;
    const jchar *year = NULL;
    const jchar *comment = NULL;
    if (id3tagTitle) {
        title = (*env)->GetStringChars(env, id3tagTitle, NULL);
    }
    if (id3tagArtist) {
        artist = (*env)->GetStringChars(env, id3tagArtist, NULL);
    }
    if (id3tagAlbum) {
        album = (*env)->GetStringChars(env, id3tagAlbum, NULL);
    }
    if (id3tagYear) {
        year = (*env)->GetStringChars(env, id3tagYear, NULL);
    }
    if (id3tagComment) {
        comment = (*env)->GetStringChars(env, id3tagComment, NULL);
    }

    if (title || artist || album || year || comment) {
        id3tag_init(glf);

        if (title) {
            id3tag_set_title(glf, (const char *) title);
            (*env)->ReleaseStringChars(env, id3tagTitle, title);
        }
        if (artist) {
            id3tag_set_artist(glf, (const char *) artist);
            (*env)->ReleaseStringChars(env, id3tagArtist, artist);
        }
        if (album) {
            id3tag_set_album(glf, (const char *) album);
            (*env)->ReleaseStringChars(env, id3tagAlbum, album);
        }
        if (year) {
            id3tag_set_year(glf, (const char *) year);
            (*env)->ReleaseStringChars(env, id3tagYear, year);
        }
        if (comment) {
            id3tag_set_comment(glf, (const char *) comment);
            (*env)->ReleaseStringChars(env, id3tagComment, comment);
        }
    }

    lame_init_params(glf);


    return glf;
}

JNIEXPORT void JNICALL Java_moe_ore_silk_LameCoder_initDecoder
        (JNIEnv *env, jclass cls) {
    hip = hip_decode_init();
}

JNIEXPORT void JNICALL Java_moe_ore_silk_LameCoder_closeDecoder
        (JNIEnv *env, jclass cls) {
    hip_decode_exit(hip);
}

JNIEXPORT jint JNICALL Java_moe_ore_silk_LameCoder_decodeFile
        (JNIEnv *env, jclass cls, jstring source, jstring dest) {
    const char *source_path, *target_path;
    mp3data_struct mp3data;
    source_path = (*env)->GetStringUTFChars(env, source, NULL);
    target_path = (*env)->GetStringUTFChars(env, dest, NULL);

    FILE *input_file, *output_file;
    input_file = fopen(source_path, "rb");
    output_file = fopen(target_path, "wb");

    unsigned char input[BUFFER_SIZE];
    short output_l[BUFFER_SIZE * 20];
    short output_r[BUFFER_SIZE * 20];
    memset(output_l, 0, BUFFER_SIZE * 20);
    memset(output_r, 0, BUFFER_SIZE * 20);
    size_t nb_read = 0;
    size_t nb_write = 0;
    size_t total_size = 0;

    nb_read = fread(input, 1, BUFFER_SIZE, input_file);
    while (nb_read) {
        nb_write = hip_decode_headers(hip, input, nb_read, output_l, output_r, &mp3data);
        total_size += nb_write;
        fwrite(output_l, nb_write, sizeof(short), output_file);
        nb_read = fread(input, 1, BUFFER_SIZE, input_file);
    }

    if (total_size == 0) {
        memset(input, 0, BUFFER_SIZE);
        nb_read = 10;
        nb_write = hip_decode_headers(hip, input, nb_read, output_l, output_r, &mp3data);
        fwrite(output_l, nb_write, sizeof(short), output_file);
    }

    fclose(input_file);
    fclose(output_file);
    return mp3data.samplerate;
}

void close(
        lame_global_flags *glf) {
    lame_close(glf);
    glf = NULL;

}