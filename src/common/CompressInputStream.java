package common;

import java.io.*;

public class CompressInputStream extends FilterInputStream {

  /*
    Classe Java que converte fluxos de byte u-Law em fluxos de byte PCM

    Baseada na classe disponível em
    http://thorntonzone.com/manuals/Compression/Fax,%20IBM%20MMR/MMSC/mmsc/uk/co/mmscomputing/sound/
    sob a licença GNU Lesser General Public

    Mathematical Tools in Signal Processing with C++ and Java Simulations
    by Willi-Hans Steeb
    International School for Scientific Computing

  */

    static private Compressor ulawcompressor = new uLawCompressor();

    private Compressor compressor = null;

    public CompressInputStream(InputStream in) throws IOException {
        super(in);
        compressor = ulawcompressor;
    }

    public int read(byte[] b) throws IOException {
        return read(b,0,b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int i, sample;
        byte[] inb;

        inb = new byte[len << 1];
        len = in.read(inb);

        if (len == -1) {
            return -1;
        }

        i = 0;

        while (i < len) {
            sample = (inb[i++] & 0x00FF);
            sample |= (inb[i++] << 8);
            b[off++] = (byte) compressor.compress((short) sample);
        }
        return len >> 1;
    }
}

abstract class Compressor {
    protected abstract int compress(short sample);
}

class uLawCompressor extends Compressor {

    static final int cClip = 32635;
    static final int cBias = 0x84;

    int[] uLawCompressTable = {
            0,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,
            4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
            5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
            5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
            6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7
    };

    protected int compress(short sample) {
        int sign;
        int exponent;
        int mantissa;
        int compressedByte;

        sign = (sample >> 8) & 0x80;
        if (sign != 0) {
            sample *= -1;
        }

        if (sample > cClip) {
            sample = cClip;
        }
        sample += cBias;

        exponent = uLawCompressTable[(sample >> 7) & 0x00FF];
        mantissa = (sample >> (exponent + 3)) & 0x0F;
        compressedByte = ~(sign | (exponent << 4) | mantissa);
        return compressedByte & 0x000000FF;
    }
}