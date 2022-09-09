#include "moe_ore_silk_SilkCodec.h"

JNIEXPORT jint JNICALL Java_moe_ore_silk_SilkCodec_nativeEncodeSilk
        (JNIEnv * env, jclass clz, jstring input, jstring output, jint rate) {

    const char *_in = (*env)->GetStringUTFChars(env, input, JNI_FALSE);
    const char *_out = (*env)->GetStringUTFChars(env, output, JNI_FALSE);

    unsigned long tottime, starttime;
    //double    filetime;
    size_t    counter;
    SKP_int32 k, totPackets, totActPackets, ret;
    SKP_int16 nBytes;
    double    sumBytes, sumActBytes,/*avg_rate, act_rate, */nrg;
    SKP_uint8 payload[ MAX_BYTES_PER_FRAME * MAX_INPUT_FRAMES ];
    SKP_int16 in[ FRAME_LENGTH_MS * MAX_API_FS_KHZ * MAX_INPUT_FRAMES ];
    char      speechInFileName[ 150 ], bitOutFileName[ 150 ];
    FILE      *bitOutFile, *speechInFile;
    SKP_int32 encSizeBytes;
    void      *psEnc;
#ifdef _SYSTEM_IS_BIG_ENDIAN
    SKP_int16 nBytes_LE;
#endif

    /* default settings */
    SKP_int32 API_fs_Hz = rate;
    SKP_int32 max_internal_fs_Hz = 0;
    SKP_int32 targetRate_bps = rate;
    SKP_int32 smplsSinceLastPacket, packetSize_ms = 20;
    SKP_int32 frameSizeReadFromFile_ms = 20;
    SKP_int32 packetLoss_perc = 0;
#if LOW_COMPLEXITY_ONLY
    SKP_int32 complexity_mode = 0;
#else
    SKP_int32 complexity_mode = 2;
#endif
    SKP_int32 DTX_enabled = 0, INBandFEC_enabled = 0;
    SKP_SILK_SDK_EncControlStruct encControl; // Struct for input to encoder
    SKP_SILK_SDK_EncControlStruct encStatus;  // Struct for status of encoder

    strcpy( speechInFileName, _in);
    strcpy( bitOutFileName,   _out);

    /* If no max internal is specified, set to minimum of API fs and 24 kHz */
    if( max_internal_fs_Hz == 0 ) {
        max_internal_fs_Hz = rate;
        if( API_fs_Hz < max_internal_fs_Hz ) {
            max_internal_fs_Hz = API_fs_Hz;
        }
    }

    /*
    if( !quiet ) {*/
    //printf("********** Silk Encoder (Fixed Point) v %s ********************\n", SKP_Silk_SDK_get_version());
    //printf("********** Compiled for %d bit cpu ******************************* \n", (int)sizeof(void*) * 8 );
    //printf( "Input:                          %s\n",     speechInFileName );
    //printf( "Output:                         %s\n",     bitOutFileName );
    //printf( "API sampling rate:              %d Hz\n",  API_fs_Hz );
    //printf( "Maximum internal sampling rate: %d Hz\n",  max_internal_fs_Hz );
    //printf( "Packet interval:                %d ms\n",  packetSize_ms );
    //printf( "Inband FEC used:                %d\n",     INBandFEC_enabled );
    //printf( "DTX used:                       %d\n",     DTX_enabled );
    //printf( "Complexity:                     %d\n",     complexity_mode );
    //printf( "Target bitrate:                 %d bps\n", targetRate_bps );
    /*}*/

    speechInFile = fopen( speechInFileName, "rb" );
    if( speechInFile == NULL ) {
        // printf( "Error: could not open input file %s\n", speechInFileName );
        return -1;
    }
    bitOutFile = fopen( bitOutFileName, "wb" );
    if( bitOutFile == NULL ) {
        //printf( "Error: could not open output file %s\n", bitOutFileName );
        //exit( 0 );
        return -2;
    }

    /* Add Silk header to stream */
    {
        static const char Tencent_break[] = "";
        fwrite(Tencent_break, sizeof(char), strlen(Tencent_break), bitOutFile);

        static const char Silk_header[] = "#!SILK_V3";
        fwrite( Silk_header, sizeof( char ), strlen( Silk_header ), bitOutFile );
    }

    ret = SKP_Silk_SDK_Get_Encoder_Size( &encSizeBytes );
    if( ret ) {
        //printf( "\nError: SKP_Silk_create_encoder returned %d\n", ret );
        //exit( 0 );
        return -3;
    }

    psEnc = malloc( encSizeBytes );

    /* Reset Encoder */
    ret = SKP_Silk_SDK_InitEncoder( psEnc, &encStatus );
    if( ret ) {
        //printf( "\nError: SKP_Silk_reset_encoder returned %d\n", ret );
        //exit( 0 );
        return ret;
    }

    /* Set Encoder parameters */
    encControl.API_sampleRate        = API_fs_Hz;
    encControl.maxInternalSampleRate = max_internal_fs_Hz;
    encControl.packetSize            = ( packetSize_ms * API_fs_Hz ) / 1000;
    encControl.packetLossPercentage  = packetLoss_perc;
    encControl.useInBandFEC          = INBandFEC_enabled;
    encControl.useDTX                = DTX_enabled;
    encControl.complexity            = complexity_mode;
    encControl.bitRate               = ( targetRate_bps > 0 ? targetRate_bps : 0 );

    if( API_fs_Hz > MAX_API_FS_KHZ * 1000 || API_fs_Hz < 0 ) {
        //printf( "\nError: API sampling rate = %d out of range, valid range 8000 - 48000 \n \n", API_fs_Hz );
        //exit( 0 );
        return -5;
    }

    tottime              = 0;
    totPackets           = 0;
    totActPackets        = 0;
    smplsSinceLastPacket = 0;
    sumBytes             = 0.0;
    sumActBytes          = 0.0;
    smplsSinceLastPacket = 0;

    while( 1 ) {
        /* Read input from file */
        counter = fread( in, sizeof( SKP_int16 ), ( frameSizeReadFromFile_ms * API_fs_Hz ) / 1000, speechInFile );
#ifdef _SYSTEM_IS_BIG_ENDIAN
        swap_endian( in, counter );
#endif
        if( ( SKP_int )counter < ( ( frameSizeReadFromFile_ms * API_fs_Hz ) / 1000 ) ) {
            break;
        }

        /* max payload size */
        nBytes = MAX_BYTES_PER_FRAME * MAX_INPUT_FRAMES;

        starttime = GetHighResolutionTime();

        /* Silk Encoder */
        ret = SKP_Silk_SDK_Encode( psEnc, &encControl, in, (SKP_int16)counter, payload, &nBytes );
        if( ret ) {
            //printf( "\nSKP_Silk_Encode returned %d", ret );
        }

        tottime += GetHighResolutionTime() - starttime;

        /* Get packet size */
        packetSize_ms = ( SKP_int )( ( 1000 * ( SKP_int32 )encControl.packetSize ) / encControl.API_sampleRate );

        smplsSinceLastPacket += ( SKP_int )counter;

        if( ( ( 1000 * smplsSinceLastPacket ) / API_fs_Hz ) == packetSize_ms ) {
            /* Sends a dummy zero size packet in case of DTX period  */
            /* to make it work with the decoder test program.        */
            /* In practice should be handled by RTP sequence numbers */
            totPackets++;
            sumBytes  += nBytes;
            nrg = 0.0;
            for( k = 0; k < ( SKP_int )counter; k++ ) {
                nrg += in[ k ] * (double)in[ k ];
            }
            if( ( nrg / ( SKP_int )counter ) > 1e3 ) {
                sumActBytes += nBytes;
                totActPackets++;
            }

            /* Write payload size */
#ifdef _SYSTEM_IS_BIG_ENDIAN
            nBytes_LE = nBytes;
            swap_endian( &nBytes_LE, 1 );
            fwrite( &nBytes_LE, sizeof( SKP_int16 ), 1, bitOutFile );
#else
            fwrite( &nBytes, sizeof( SKP_int16 ), 1, bitOutFile );
#endif

            /* Write payload */
            fwrite( payload, sizeof( SKP_uint8 ), nBytes, bitOutFile );

            smplsSinceLastPacket = 0;

            //if( !quiet ) {
            //fprintf( stderr, "\rPackets encoded:                %d", totPackets );
            //}
        }
    }

    /* Write dummy because it can not end with 0 bytes */
    nBytes = -1;

    /* Write payload size */
    //if( !tencent ) {
    //   fwrite( &nBytes, sizeof( SKP_int16 ), 1, bitOutFile );
    //}

    /* Free Encoder */
    free( psEnc );

    fclose( speechInFile );
    fclose( bitOutFile );

    //filetime  = totPackets * 1e-3 * packetSize_ms;
    //avg_rate  = 8.0 / packetSize_ms * sumBytes       / totPackets;
    //act_rate  = 8.0 / packetSize_ms * sumActBytes    / totActPackets;

    //printf( "\nFile length:                    %.3f s", filetime );
    //printf( "\nTime for encoding:              %.3f s (%.3f%% of realtime)", 1e-6 * tottime, 1e-4 * tottime / filetime );
    //printf( "\nAverage bitrate:                %.3f kbps", avg_rate  );
    //printf( "\nActive bitrate:                 %.3f kbps", act_rate  );
    //printf( "\n\n" );
    return 200;
}

JNIEXPORT jint JNICALL Java_moe_ore_silk_SilkCodec_nativeDecodeSilk
        (JNIEnv * env, jclass clz, jstring input, jstring output, jint rate) {

    const char *_in = (*env)->GetStringUTFChars(env, input, JNI_FALSE);
    const char *_out = (*env)->GetStringUTFChars(env, output, JNI_FALSE);

    unsigned long tottime, starttime;
    //double    filetime;
    size_t    counter;
    SKP_int32 /*args,*/ totPackets, i, k;
    SKP_int16 ret, len, tot_len;
    SKP_int16 nBytes;
    SKP_uint8 payload[    MAX_BYTES_PER_FRAME * MAX_INPUT_FRAMES * ( MAX_LBRR_DELAY + 1 ) ];
    SKP_uint8 *payloadEnd = NULL, *payloadToDec = NULL;
    SKP_uint8 FECpayload[ MAX_BYTES_PER_FRAME * MAX_INPUT_FRAMES ], *payloadPtr;
    SKP_int16 nBytesFEC;
    SKP_int16 nBytesPerPacket[ MAX_LBRR_DELAY + 1 ], totBytes;
    SKP_int16 out[ ( ( FRAME_LENGTH_MS * MAX_API_FS_KHZ ) << 1 ) * MAX_INPUT_FRAMES ], *outPtr;
    char      speechOutFileName[ 150 ], bitInFileName[ 150 ];
    FILE      *bitInFile, *speechOutFile;
    SKP_int32 packetSize_ms=0, API_Fs_Hz = 0;
    SKP_int32 decSizeBytes;
    void      *psDec;
    SKP_float loss_prob;
    SKP_int32 frames, lost;
    SKP_SILK_SDK_DecControlStruct DecControl;

    /* default settings */
    loss_prob = 0.0f;

    /* get arguments */
    strcpy( bitInFileName, _in );
    strcpy( speechOutFileName, _out);
    API_Fs_Hz = rate;

    /*
    if( !quiet ) {
        printf("********** Silk Decoder (Fixed Point) v %s ********************\n", SKP_Silk_SDK_get_version());
        printf("********** Compiled for %d bit cpu *******************************\n", (int)sizeof(void*) * 8 );
        printf( "Input:                       %s\n", bitInFileName );
        printf( "Output:                      %s\n", speechOutFileName );
    }*/

    /* Open files */
    bitInFile = fopen( bitInFileName, "rb" );
    if( bitInFile == NULL ) {
        //printf( "Error: could not open input file %s\n", bitInFileName );
        //exit( 0 );
        return -1;
    }

    /* Check Silk header */
    {
        char header_buf[ 50 ];
        fread(header_buf, sizeof(char), 1, bitInFile);
        header_buf[ strlen( "" ) ] = '\0'; /* Terminate with a null character */
        if( strcmp( header_buf, "" ) != 0 ) {
            counter = fread( header_buf, sizeof( char ), strlen( "!SILK_V3" ), bitInFile );
            header_buf[ strlen( "!SILK_V3" ) ] = '\0'; /* Terminate with a null character */
            if( strcmp( header_buf, "!SILK_V3" ) != 0 ) {
                /* Non-equal strings */
                //printf( "Error: Wrong Header %s\n", header_buf );
                //exit( 0 );
                return -2;
            }
        } else {
            counter = fread( header_buf, sizeof( char ), strlen( "#!SILK_V3" ), bitInFile );
            header_buf[ strlen( "#!SILK_V3" ) ] = '\0'; /* Terminate with a null character */
            if( strcmp( header_buf, "#!SILK_V3" ) != 0 ) {
                /* Non-equal strings */
                //printf( "Error: Wrong Header %s\n", header_buf );
                //exit( 0 );
                return -3;
            }
        }
    }

    speechOutFile = fopen( speechOutFileName, "wb" );
    if( speechOutFile == NULL ) {
        //printf( "Error: could not open output file %s\n", speechOutFileName );
        //exit( 0 );
        return -4;
    }

    /* Set the samplingrate that is requested for the output */
    if( API_Fs_Hz == 0 ) {
        DecControl.API_sampleRate = 24000;
    } else {
        DecControl.API_sampleRate = API_Fs_Hz;
    }

    /* Initialize to one frame per packet, for proper concealment before first packet arrives */
    DecControl.framesPerPacket = 1;

    /* Create decoder */
    ret = SKP_Silk_SDK_Get_Decoder_Size( &decSizeBytes );
    /*if( ret ) {
        // printf( "\nSKP_Silk_SDK_Get_Decoder_Size returned %d", ret );
    }*/
    psDec = malloc( decSizeBytes );

    /* Reset decoder */
    ret = SKP_Silk_SDK_InitDecoder( psDec );
    /*if( ret ) {
        //printf( "\nSKP_Silk_InitDecoder returned %d", ret );
    }*/

    totPackets = 0;
    tottime    = 0;
    payloadEnd = payload;

    /* Simulate the jitter buffer holding MAX_FEC_DELAY packets */
    for( i = 0; i < MAX_LBRR_DELAY; i++ ) {
        /* Read payload size */
        counter = fread( &nBytes, sizeof( SKP_int16 ), 1, bitInFile );
#ifdef _SYSTEM_IS_BIG_ENDIAN
        swap_endian( &nBytes, 1 );
#endif
        /* Read payload */
        counter = fread( payloadEnd, sizeof( SKP_uint8 ), nBytes, bitInFile );

        if( ( SKP_int16 )counter < nBytes ) {
            break;
        }
        nBytesPerPacket[ i ] = nBytes;
        payloadEnd          += nBytes;
        totPackets++;
    }

    while( 1 ) {
        /* Read payload size */
        counter = fread( &nBytes, sizeof( SKP_int16 ), 1, bitInFile );
#ifdef _SYSTEM_IS_BIG_ENDIAN
        swap_endian( &nBytes, 1 );
#endif
        if( nBytes < 0 || counter < 1 ) {
            break;
        }

        /* Read payload */
        counter = fread( payloadEnd, sizeof( SKP_uint8 ), nBytes, bitInFile );
        if( ( SKP_int16 )counter < nBytes ) {
            break;
        }

        /* Simulate losses */
        rand_seed = SKP_RAND( rand_seed );
        if( ( ( ( float )( ( rand_seed >> 16 ) + ( 1 << 15 ) ) ) / 65535.0f >= ( loss_prob / 100.0f ) ) && ( counter > 0 ) ) {
            nBytesPerPacket[ MAX_LBRR_DELAY ] = nBytes;
            payloadEnd                       += nBytes;
        } else {
            nBytesPerPacket[ MAX_LBRR_DELAY ] = 0;
        }

        if( nBytesPerPacket[ 0 ] == 0 ) {
            /* Indicate lost packet */
            lost = 1;

            /* Packet loss. Search after FEC in next packets. Should be done in the jitter buffer */
            payloadPtr = payload;
            for( i = 0; i < MAX_LBRR_DELAY; i++ ) {
                if( nBytesPerPacket[ i + 1 ] > 0 ) {
                    starttime = GetHighResolutionTime();
                    SKP_Silk_SDK_search_for_LBRR( payloadPtr, nBytesPerPacket[ i + 1 ], ( i + 1 ), FECpayload, &nBytesFEC );
                    tottime += GetHighResolutionTime() - starttime;
                    if( nBytesFEC > 0 ) {
                        payloadToDec = FECpayload;
                        nBytes = nBytesFEC;
                        lost = 0;
                        break;
                    }
                }
                payloadPtr += nBytesPerPacket[ i + 1 ];
            }
        } else {
            lost = 0;
            nBytes = nBytesPerPacket[ 0 ];
            payloadToDec = payload;
        }

        /* Silk decoder */
        outPtr = out;
        tot_len = 0;
        starttime = GetHighResolutionTime();

        if( lost == 0 ) {
            /* No Loss: Decode all frames in the packet */
            frames = 0;
            do {
                /* Decode 20 ms */
                ret = SKP_Silk_SDK_Decode( psDec, &DecControl, 0, payloadToDec, nBytes, outPtr, &len );
                /*if( ret ) {
                    printf( "\nSKP_Silk_SDK_Decode returned %d", ret );
                }*/

                frames++;
                outPtr  += len;
                tot_len += len;
                if( frames > MAX_INPUT_FRAMES ) {
                    /* Hack for corrupt stream that could generate too many frames */
                    outPtr  = out;
                    tot_len = 0;
                    frames  = 0;
                }
                /* Until last 20 ms frame of packet has been decoded */
            } while( DecControl.moreInternalDecoderFrames );
        } else {
            /* Loss: Decode enough frames to cover one packet duration */
            for( i = 0; i < DecControl.framesPerPacket; i++ ) {
                /* Generate 20 ms */
                ret = SKP_Silk_SDK_Decode( psDec, &DecControl, 1, payloadToDec, nBytes, outPtr, &len );
                /*if( ret ) {
                    printf( "\nSKP_Silk_Decode returned %d", ret );
                }*/
                outPtr  += len;
                tot_len += len;
            }
        }

        packetSize_ms = tot_len / ( DecControl.API_sampleRate / 1000 );
        tottime += GetHighResolutionTime() - starttime;
        totPackets++;

        /* Write output to file */
#ifdef _SYSTEM_IS_BIG_ENDIAN
        swap_endian( out, tot_len );
#endif
        fwrite( out, sizeof( SKP_int16 ), tot_len, speechOutFile );

        /* Update buffer */
        totBytes = 0;
        for( i = 0; i < MAX_LBRR_DELAY; i++ ) {
            totBytes += nBytesPerPacket[ i + 1 ];
        }
        /* Check if the received totBytes is valid */
        if (totBytes < 0 || totBytes > sizeof(payload))
        {
            // fprintf( stderr, "\rPackets decoded:             %d", totPackets );
            return -5;
        }
        SKP_memmove( payload, &payload[ nBytesPerPacket[ 0 ] ], totBytes * sizeof( SKP_uint8 ) );
        payloadEnd -= nBytesPerPacket[ 0 ];
        SKP_memmove( nBytesPerPacket, &nBytesPerPacket[ 1 ], MAX_LBRR_DELAY * sizeof( SKP_int16 ) );

        /*if( !quiet ) {
            fprintf( stderr, "\rPackets decoded:             %d", totPackets );
        }*/
    }

    /* Empty the recieve buffer */
    for( k = 0; k < MAX_LBRR_DELAY; k++ ) {
        if( nBytesPerPacket[ 0 ] == 0 ) {
            /* Indicate lost packet */
            lost = 1;

            /* Packet loss. Search after FEC in next packets. Should be done in the jitter buffer */
            payloadPtr = payload;
            for( i = 0; i < MAX_LBRR_DELAY; i++ ) {
                if( nBytesPerPacket[ i + 1 ] > 0 ) {
                    starttime = GetHighResolutionTime();
                    SKP_Silk_SDK_search_for_LBRR( payloadPtr, nBytesPerPacket[ i + 1 ], ( i + 1 ), FECpayload, &nBytesFEC );
                    tottime += GetHighResolutionTime() - starttime;
                    if( nBytesFEC > 0 ) {
                        payloadToDec = FECpayload;
                        nBytes = nBytesFEC;
                        lost = 0;
                        break;
                    }
                }
                payloadPtr += nBytesPerPacket[ i + 1 ];
            }
        } else {
            lost = 0;
            nBytes = nBytesPerPacket[ 0 ];
            payloadToDec = payload;
        }

        /* Silk decoder */
        outPtr  = out;
        tot_len = 0;
        starttime = GetHighResolutionTime();

        if( lost == 0 ) {
            /* No loss: Decode all frames in the packet */
            frames = 0;
            do {
                /* Decode 20 ms */
                ret = SKP_Silk_SDK_Decode( psDec, &DecControl, 0, payloadToDec, nBytes, outPtr, &len );
                /*if( ret ) {
                    printf( "\nSKP_Silk_SDK_Decode returned %d", ret );
                }*/

                frames++;
                outPtr  += len;
                tot_len += len;
                if( frames > MAX_INPUT_FRAMES ) {
                    /* Hack for corrupt stream that could generate too many frames */
                    outPtr  = out;
                    tot_len = 0;
                    frames  = 0;
                }
                /* Until last 20 ms frame of packet has been decoded */
            } while( DecControl.moreInternalDecoderFrames );
        } else {
            /* Loss: Decode enough frames to cover one packet duration */

            /* Generate 20 ms */
            for( i = 0; i < DecControl.framesPerPacket; i++ ) {
                ret = SKP_Silk_SDK_Decode( psDec, &DecControl, 1, payloadToDec, nBytes, outPtr, &len );
                /*if( ret ) {
                    printf( "\nSKP_Silk_Decode returned %d", ret );
                }*/
                outPtr  += len;
                tot_len += len;
            }
        }

        packetSize_ms = tot_len / ( DecControl.API_sampleRate / 1000 );
        tottime += GetHighResolutionTime() - starttime;
        totPackets++;

        /* Write output to file */
#ifdef _SYSTEM_IS_BIG_ENDIAN
        swap_endian( out, tot_len );
#endif
        fwrite( out, sizeof( SKP_int16 ), tot_len, speechOutFile );

        /* Update Buffer */
        totBytes = 0;
        for( i = 0; i < MAX_LBRR_DELAY; i++ ) {
            totBytes += nBytesPerPacket[ i + 1 ];
        }

        /* Check if the received totBytes is valid */
        if (totBytes < 0 || totBytes > sizeof(payload))
        {

            //fprintf( stderr, "\rPackets decoded:              %d", totPackets );
            return -6;
        }

        SKP_memmove( payload, &payload[ nBytesPerPacket[ 0 ] ], totBytes * sizeof( SKP_uint8 ) );
        payloadEnd -= nBytesPerPacket[ 0 ];
        SKP_memmove( nBytesPerPacket, &nBytesPerPacket[ 1 ], MAX_LBRR_DELAY * sizeof( SKP_int16 ) );

        /*if( !quiet ) {
            fprintf( stderr, "\rPackets decoded:              %d", totPackets );
        }*/
    }

    /*
    if( !quiet ) {
        printf( "\nDecoding Finished \n" );
    }*/

    /* Free decoder */
    free( psDec );

    /* Close files */
    fclose( speechOutFile );
    fclose( bitInFile );

    /*filetime = totPackets * 1e-3 * packetSize_ms;
    if( !quiet ) {
        printf("\nFile length:                 %.3f s", filetime);
        printf("\nTime for decoding:           %.3f s (%.3f%% of realtime)", 1e-6 * tottime, 1e-4 * tottime / filetime);
        printf("\n\n");
    } else {
        /* print time and % of realtime
        printf( "%.3f %.3f %d\n", 1e-6 * tottime, 1e-4 * tottime / filetime, totPackets );
    }
    return 0;*/
    return 200;
}