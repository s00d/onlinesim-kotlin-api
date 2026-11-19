package ru.onlinesim.kotlin

import ru.onlinesim.FreeListOptions

/** Public free numbers: `client.free`. No key needed. */
public class FreeApi internal constructor(
    private val java: ru.onlinesim.FreeApi,
    private val io: Io,
) {
    /**
     * Countries, the numbers of the selected country and the SMS of the selected number, in one call.
     * Unset (`null`) options are not sent.
     *
     * @param country country code; server default 44
     * @param number number without the country code, as the keys of `numbers()`
     * @param page page of `messages()`
     * @param count SMS per page, 10–500
     */
    public suspend fun list(
        country: Int? = null,
        number: String? = null,
        page: Int? = null,
        count: Int? = null,
    ): FreeList {
        var options = FreeListOptions.defaults()
        country?.let { options = options.country(it) }
        number?.let { options = options.number(it) }
        page?.let { options = options.page(it) }
        count?.let { options = options.count(it) }
        return io { java.list(options) }
    }
}
