package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Xml
import com.example.model.CityMarket
import com.example.model.DealMode
import com.example.model.LiveListing
import com.example.model.ResaleGuideCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed interface RssScanResult {
    data class Success(val listings: List<LiveListing>, val feedUrl: String) : RssScanResult
    data class Unavailable(val message: String, val browserUrl: String) : RssScanResult
}

object MarketplaceEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val CITY_MARKETS = listOf(
        CityMarket("sfbay", "SF Bay Area", "CA"),
        CityMarket("losangeles", "Los Angeles", "CA"),
        CityMarket("sandiego", "San Diego", "CA"),
        CityMarket("newyork", "New York City", "NY"),
        CityMarket("chicago", "Chicago", "IL"),
        CityMarket("austin", "Austin", "TX"),
        CityMarket("dallas", "Dallas / Fort Worth", "TX"),
        CityMarket("houston", "Houston", "TX"),
        CityMarket("miami", "South Florida / Miami", "FL"),
        CityMarket("orlando", "Orlando", "FL"),
        CityMarket("tampa", "Tampa Bay", "FL"),
        CityMarket("seattle", "Seattle / Tacoma", "WA"),
        CityMarket("portland", "Portland", "OR"),
        CityMarket("denver", "Denver", "CO"),
        CityMarket("phoenix", "Phoenix", "AZ"),
        CityMarket("atlanta", "Atlanta", "GA"),
        CityMarket("boston", "Boston", "MA"),
        CityMarket("philadelphia", "Philadelphia", "PA"),
        CityMarket("nashville", "Nashville", "TN"),
        CityMarket("charlotte", "Charlotte", "NC"),
        CityMarket("minneapolis", "Minneapolis / St. Paul", "MN"),
        CityMarket("detroit", "Detroit", "MI"),
        CityMarket("lasvegas", "Las Vegas", "NV"),
        CityMarket("saltlakecity", "Salt Lake City", "UT")
    )

    val RESALE_CATEGORIES = listOf(
        "All",
        "Furniture",
        "Power Tools",
        "Electronics",
        "Appliances",
        "Bikes & Fitness",
        "Patio & Yard",
        "Collectibles",
        "Baby & Kids"
    )

    val RESALE_GUIDE_ITEMS = listOf(
        ResaleGuideCategory(
            id = "furniture",
            name = "Solid Wood & Mid-Century Furniture",
            searchKeywords = "solid wood dresser",
            defaultFreeSearch = true,
            suggestedMaxCash = 60,
            typicalResaleRange = "$150 – $550",
            flipSpeed = "1–4 days",
            whatToLookFor = "Dovetail drawer joints, real walnut/oak/teak grain, maker stamps (Broyhill, Lane, West Elm, Crate & Barrel), moving-out curb alerts.",
            inspectionRedFlags = "Swollen particle board, peeling laminate veneer, musty smoke odor, broken drawer tracks."
        ),
        ResaleGuideCategory(
            id = "tools",
            name = "Cordless Power Tools & Combos",
            searchKeywords = "dewalt milwaukee brushless",
            defaultFreeSearch = false,
            suggestedMaxCash = 120,
            typicalResaleRange = "$140 – $380",
            flipSpeed = "Same day – 2 days",
            whatToLookFor = "DeWalt XR, Milwaukee M18 Fuel, Makita 18V LXT kits with batteries and charger. Estate cleanouts and retired contractor lots.",
            inspectionRedFlags = "Dead/flashing battery packs, burnt motor smell, stripped chuck, missing charger."
        ),
        ResaleGuideCategory(
            id = "appliances",
            name = "Countertop Espresso & Mixers",
            searchKeywords = "kitchenaid breville espresso",
            defaultFreeSearch = false,
            suggestedMaxCash = 100,
            typicalResaleRange = "$180 – $480",
            flipSpeed = "1–3 days",
            whatToLookFor = "KitchenAid Artisan/Pro stand mixers, Breville Barista Express, Vitamix blenders, Dyson vacuums.",
            inspectionRedFlags = "Grinding worm gear on mixers, leaking boiler seals on espresso machines, cracked Vitamix pitcher base."
        ),
        ResaleGuideCategory(
            id = "patio",
            name = "Patio Sets, Grills & Lawn Gear",
            searchKeywords = "weber grill patio set",
            defaultFreeSearch = true,
            suggestedMaxCash = 80,
            typicalResaleRange = "$120 – $400",
            flipSpeed = "1–3 days",
            whatToLookFor = "Weber Genesis/Spirit grills, cast aluminum or teak patio sets, Honda/EGO cordless mowers left behind on move-outs.",
            inspectionRedFlags = "Rusted-through firebox bottom, missing EGO battery, brittle dry-rotted wicker."
        ),
        ResaleGuideCategory(
            id = "electronics",
            name = "Gaming Consoles & Vintage Audio",
            searchKeywords = "nintendo switch receiver",
            defaultFreeSearch = false,
            suggestedMaxCash = 110,
            typicalResaleRange = "$160 – $360",
            flipSpeed = "Same day – 2 days",
            whatToLookFor = "Nintendo Switch OLED, PS5, 1970s Marantz/Pioneer/Sansui stereo receivers, Sony/Canon mirrorless cameras.",
            inspectionRedFlags = "Banned console serials, severe Joy-Con drift, HDMI port wobble, scratchy audio channels."
        ),
        ResaleGuideCategory(
            id = "bikes",
            name = "Brand-Name Bikes & Home Gym",
            searchKeywords = "trek specialized dumbbells",
            defaultFreeSearch = false,
            suggestedMaxCash = 120,
            typicalResaleRange = "$180 – $500",
            flipSpeed = "2–5 days",
            whatToLookFor = "Trek, Specialized, Cannondale, Giant bikes; Bowflex SelectTech adjustable dumbbells; Concept2 rowers.",
            inspectionRedFlags = "Hairline frame cracks near welds, seized seatposts, jammed Bowflex selector dials."
        ),
        ResaleGuideCategory(
            id = "baby",
            name = "Premium Strollers & Wagons",
            searchKeywords = "uppababy wonderfold stroller",
            defaultFreeSearch = false,
            suggestedMaxCash = 150,
            typicalResaleRange = "$280 – $650",
            flipSpeed = "1–4 days",
            whatToLookFor = "UPPAbaby Vista/Cruz, Bugaboo, Wonderfold wagons, Snoo bassinets from families upgrading or clearing space.",
            inspectionRedFlags = "Bent folding frame, heavy sun-faded canopy, missing toddler seat clips."
        ),
        ResaleGuideCategory(
            id = "free_curb",
            name = "Zero-Dollar Curb & Moving Alerts",
            searchKeywords = "moving free pickup",
            defaultFreeSearch = true,
            suggestedMaxCash = 0,
            typicalResaleRange = "$40 – $250",
            flipSpeed = "Same day",
            whatToLookFor = "Moving boxes, shelving racks, office chairs (Herman Miller/Steelcase), mini fridges, mirrors, weights.",
            inspectionRedFlags = "Upholstered items with pet damage/bedbug risk, non-cooling fridges (always wait 10 min plugged in)."
        )
    )

    private fun encode(value: String): String =
        URLEncoder.encode(value.trim(), "UTF-8")

    fun buildFacebookMarketplaceUrl(
        keywords: String,
        dealMode: DealMode,
        maxPriceDollars: Int
    ): String {
        val q = keywords.trim()
        val effectiveMax = if (dealMode == DealMode.FREE_ONLY) 0 else maxPriceDollars.coerceAtLeast(1)
        val base = "https://www.facebook.com/marketplace/search/?"
        val params = buildList {
            if (q.isNotEmpty()) add("query=${encode(q)}")
            add("maxPrice=$effectiveMax")
            if (dealMode == DealMode.FREE_ONLY) add("minPrice=0")
            add("sortBy=creation_time_descend")
            add("deliveryMethod=local_pick_up")
        }
        return base + params.joinToString("&")
    }

    fun buildCraigslistUrl(
        citySlug: String,
        keywords: String,
        dealMode: DealMode,
        maxPriceDollars: Int,
        rssFormat: Boolean = false
    ): String {
        val cleanSlug = citySlug.trim().lowercase().replace(Regex("[^a-z0-9]"), "").ifEmpty { "sfbay" }
        val section = if (dealMode == DealMode.FREE_ONLY) "zip" else "sss"
        val params = buildList {
            if (keywords.trim().isNotEmpty()) add("query=${encode(keywords)}")
            if (dealMode == DealMode.CASH_BARGAIN) {
                add("max_price=${maxPriceDollars.coerceAtLeast(1)}")
                add("purveyor=owner")
            }
            add("sort=date")
            if (rssFormat) add("format=rss")
        }
        return "https://$cleanSlug.craigslist.org/search/$section?${params.joinToString("&")}"
    }

    fun buildOfferUpUrl(
        keywords: String,
        dealMode: DealMode,
        maxPriceDollars: Int
    ): String {
        val q = keywords.trim().ifEmpty { if (dealMode == DealMode.FREE_ONLY) "free" else "cash" }
        val effectiveMax = if (dealMode == DealMode.FREE_ONLY) 0 else maxPriceDollars.coerceAtLeast(1)
        return "https://offerup.com/search?q=${encode(q)}&price_max=$effectiveMax&delivery_param=p&sort=newest"
    }

    fun buildNextdoorUrl(keywords: String, dealMode: DealMode): String {
        val q = keywords.trim().ifEmpty { if (dealMode == DealMode.FREE_ONLY) "free" else "sale" }
        return "https://nextdoor.com/for_sale_and_free/?query=${encode(q)}"
    }

    fun buildEbaySoldCompsUrl(keywords: String): String {
        val q = keywords.trim().ifEmpty { "vintage" }
        return "https://www.ebay.com/sch/i.html?_nkw=${encode(q)}&LH_Sold=1&LH_Complete=1&_sop=13"
    }

    fun openExternalUrl(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchCraigslistRss(
        citySlug: String,
        keywords: String,
        dealMode: DealMode,
        maxPriceDollars: Int
    ): RssScanResult = withContext(Dispatchers.IO) {
        val rssUrl = buildCraigslistUrl(citySlug, keywords, dealMode, maxPriceDollars, rssFormat = true)
        val browserUrl = buildCraigslistUrl(citySlug, keywords, dealMode, maxPriceDollars, rssFormat = false)

        try {
            val request = Request.Builder()
                .url(rssUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
                )
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext RssScanResult.Unavailable(
                        message = "Local feed returned HTTP ${response.code}. Open directly in browser to view live posts.",
                        browserUrl = browserUrl
                    )
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank() || !body.contains("<item", ignoreCase = true)) {
                    return@withContext RssScanResult.Unavailable(
                        message = "No RSS items returned for this query. Open Craigslist directly in browser to inspect live listings.",
                        browserUrl = browserUrl
                    )
                }

                val parsed = parseCraigslistRssXml(body, dealMode)
                if (parsed.isEmpty()) {
                    RssScanResult.Unavailable(
                        message = "No matching live posts found in feed. Try broader keywords or open in browser.",
                        browserUrl = browserUrl
                    )
                } else {
                    RssScanResult.Success(listings = parsed, feedUrl = rssUrl)
                }
            }
        } catch (e: Exception) {
            RssScanResult.Unavailable(
                message = "Could not reach live feed (${e.localizedMessage ?: "network check"}). Use one-tap browser launch below.",
                browserUrl = browserUrl
            )
        }
    }

    private fun parseCraigslistRssXml(xml: String, dealMode: DealMode): List<LiveListing> {
        val results = mutableListOf<LiveListing>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var insideItem = false
        var title = ""
        var link = ""
        var date = ""

        while (eventType != XmlPullParser.END_DOCUMENT && results.size < 25) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase()
                    if (tag == "item") {
                        insideItem = true
                        title = ""
                        link = ""
                        date = ""
                    } else if (insideItem) {
                        when (tag) {
                            "title" -> title = parser.nextText().trim()
                            "link" -> link = parser.nextText().trim()
                            "dc:date", "pubdate" -> date = parser.nextText().trim()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true) && insideItem) {
                        insideItem = false
                        if (title.isNotBlank() && link.isNotBlank()) {
                            val cleanTitle = decodeHtml(title)
                            val priceMatch = Regex("\\$(\\d[\\d,]*)").find(cleanTitle)
                            val parsedDollars = priceMatch?.groupValues?.getOrNull(1)
                                ?.replace(",", "")?.toIntOrNull()
                            val priceLabel = when {
                                dealMode == DealMode.FREE_ONLY || parsedDollars == 0 -> "$0 FREE"
                                parsedDollars != null -> "$$parsedDollars"
                                else -> "Cash / Best Offer"
                            }
                            val locMatch = Regex("\\(([^)]+)\\)\\s*(?:\\$\\d|$)").find(cleanTitle)
                            val location = locMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
                            val shortDate = date.take(16).replace("T", " ")

                            results.add(
                                LiveListing(
                                    id = link.hashCode().toString(),
                                    title = cleanTitle,
                                    priceText = priceLabel,
                                    parsedPriceDollars = if (dealMode == DealMode.FREE_ONLY) 0 else parsedDollars,
                                    locationText = location.ifEmpty { "Local Pickup" },
                                    publishedText = shortDate.ifEmpty { "Recent" },
                                    url = link,
                                    source = "Craigslist"
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return results
    }

    private fun decodeHtml(input: String): String {
        return input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#x0024;", "$")
            .trim()
    }
}
