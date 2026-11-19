package ru.onlinesim.kotlin

/** Rented numbers: `client.rent`. */
public class RentApi internal constructor(
    private val java: ru.onlinesim.RentApi,
    private val io: Io,
) {
    /**
     * Rents a number and charges the balance.
     *
     * @param days one of the tariff's `days` keys
     * @param extension `false` makes the rent non-extendable for good
     * @throws NoNumberException when no number is available
     */
    public suspend fun get(
        country: Int = 7,
        days: Int = 1,
        extension: Boolean = true,
    ): RentItem = io { java.get(country, days, extension) }

    /** All rents of the account. */
    public suspend fun state(): List<RentItem> = io { java.state() }

    /**
     * One rent.
     *
     * @throws ApiException `ERROR_NO_OPERATIONS` for an unknown tzid
     */
    public suspend fun stateOne(tzid: Long): RentItem = io { java.stateOne(tzid) }

    /** Extends a rent and charges the balance (only rents created with `extension = true`). */
    public suspend fun extend(
        tzid: Long,
        days: Int = 1,
    ): RentItem = io { java.extend(tzid, days) }

    /** Rent prices of every country, keyed by country code. No key needed. */
    public suspend fun tariffs(): Map<String, RentTariff> = io { java.tariffs() }

    /**
     * Rent prices of one country. No key needed.
     *
     * @throws ApiException `NO_COUNTRY` for a country without rent
     */
    public suspend fun tariffsOne(country: Int = 7): RentTariff = io { java.tariffsOne(country) }

    /**
     * Returns the number before the rent period ends.
     *
     * @throws ApiException e.g. `NO_COMPLETE_TZID`, `NO_CLOSE_RENT_US`
     */
    public suspend fun close(tzid: Long): Unit = io { java.close(tzid) }
}
