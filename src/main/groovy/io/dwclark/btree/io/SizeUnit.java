package io.dwclark.btree.io;

public enum SizeUnit {

    BYTES(1L),
    KILOS(1_024L),
    MEGAS(1_024L * 1_024L),
    GIGAS(1_024L * 1_024L * 1_024L),
    TERAS(1_024L * 1_024L * 1_024L * 1_024L),
    PETAS(1_024L * 1_024L * 1_024L * 1_024L *1_024L);

    private SizeUnit(final long divisor) {
        this.divisor = divisor;
    }

    private final long divisor;

    public long convert(final long source, final SizeUnit sourceUnit) {
        return (source * sourceUnit.divisor) / divisor;
    }

    public long toBytes(final long source) {
        return BYTES.convert(source, this);
    }

    public long toKilos(final long source) {
        return KILOS.convert(source, this);
    }

    public long toMegas(final long source) {
        return MEGAS.convert(source, this);
    }

    public long toGigas(final long source) {
        return GIGAS.convert(source, this);
    }

    public long toTeras(final long source) {
        return TERAS.convert(source, this);
    }

    public long toPetas(final long source) {
        return PETAS.convert(source, this);
    }
}
