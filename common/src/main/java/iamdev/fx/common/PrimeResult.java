package iamdev.fx.common;

import java.util.Objects;

public class PrimeResult {

    private Integer integer;

    private Integer primeFlag;

    public PrimeResult(Integer integer, Integer primeFlag) {
        this.integer = integer;
        this.primeFlag = primeFlag;
    }

    public Integer getInteger() {
        return integer;
    }

    public void setInteger(Integer integer) {
        this.integer = integer;
    }

    public Integer getPrimeFlag() {
        return primeFlag;
    }

    public void setPrimeFlag(Integer primeFlag) {
        this.primeFlag = primeFlag;
    }

    public boolean isPrime() {
        return null != this.primeFlag && 0 == this.primeFlag.intValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimeResult that = (PrimeResult) o;
        return Objects.equals(integer, that.integer) && Objects.equals(primeFlag, that.primeFlag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(integer, primeFlag);
    }

    @Override
    public String toString() {
        return "PrimeResult{" +
                "integer=" + integer +
                ", isPrime=" + isPrime() +
                '}';
    }
}
