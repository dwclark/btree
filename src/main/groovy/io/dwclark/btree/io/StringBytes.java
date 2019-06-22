package io.dwclark.btree.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

class StringBytes {

    static final int SIZE = 1_024;

    static class Encoder {
        final byte[] ary = new byte[SIZE];
        final ByteBuffer output = ByteBuffer.wrap(ary);
        
        private MutableBytes bytes;
        private CharBuffer input;
        private Charset charset;
        private long at;
        private int written;
        
        Encoder reset(final String str, final Charset charset, final long at, final MutableBytes bytes) {
            output.clear();
            this.written = 0;
            this.input = CharBuffer.wrap(str);
            this.charset = charset;
            this.at = at + 4;
            this.bytes = bytes;
            return this;
        }

        private void serialize() {
            if(output.position() > 0) {
                output.flip();
                bytes.write(at + written, ary, 0, output.limit());
                written += output.limit();
                output.position(output.limit());
                output.compact();
            }
        }
        
        void encode() {
            final CharsetEncoder encoder = charset.newEncoder();
            while(encoder.encode(input, output, true).isOverflow()) {
                serialize();
            }

            while(encoder.flush(output).isOverflow()) {
                serialize();
            }

            serialize();
            bytes.writeInt(at - 4, written);
        }
    }

    static class Decoder {
        final byte[] inputAry = new byte[SIZE];
        final ByteBuffer input = ByteBuffer.wrap(inputAry);
        final CharBuffer output = CharBuffer.allocate(SIZE / 2);

        StringBuilder builder;
        Charset charset;
        ImmutableBytes bytes;
        long at;
        int size;
        int read;

        Decoder reset(final Charset charset, final long at, final ImmutableBytes bytes) {
            input.clear();
            output.clear();

            this.size = bytes.readInt(at);
            this.builder = new StringBuilder();
            this.at = at + 4;
            this.bytes = bytes;
            this.charset = charset;
            this.read = 0;

            return this;
        }
        
        private void fillInput() {
            final int toRead = Math.min(size - read, input.remaining());
            bytes.read(at + read, inputAry, input.position(), toRead);
            read += toRead;
            input.position(input.position() + toRead);
            input.flip();
        }

        private void accumulateToBuilder() {
            output.flip();
            builder.append(output);
            output.position(output.limit());
            output.compact();
        }
        
        String decode() {
            final CharsetDecoder decoder = charset.newDecoder();
            CoderResult result;
            
            while(read < size) {
                fillInput();

                result = decoder.decode(input, output, read < size);
                accumulateToBuilder();
                input.flip();
            }

            while((result = decoder.decode(input, output, true)).isOverflow()) {
                accumulateToBuilder();
            }
            
            while((result = decoder.flush(output)).isOverflow()) {
                accumulateToBuilder();
            }

            final String ret = builder.toString();
            builder = null;
            return ret;
        }
    }

    private static final ThreadLocal<Encoder> _tlEncoder = ThreadLocal.withInitial(Encoder::new);
    
    public static void encode(final long at, final String str, final Charset charset, final MutableBytes bytes) {
        _tlEncoder.get().reset(str, charset, at, bytes).encode();
    }
    
    private static final ThreadLocal<Decoder> _tlDecoder = ThreadLocal.withInitial(Decoder::new);

    public static String decode(final long at, final Charset charset, final ImmutableBytes bytes) {
        return _tlDecoder.get().reset(charset, at, bytes).decode();
    }
}
