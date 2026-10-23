package com.takehome.drama.data.mapper

import com.takehome.drama.data.remote.dto.ShowEpisodesDto
import com.takehome.drama.data.remote.http.defaultJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EpisodeMapperTest {

    private fun parse(json: String) = defaultJson.decodeFromString<ShowEpisodesDto>(json).toDomain()

    @Test
    fun `unknown quality keys are kept, not a parse error`() {
        val episode = parse(
            """{"showId":"1","episodes":[{"number":1,"sources":{"en":{"720p":"a","2160p":"b","abc":"c"}}}]}""",
        ).episodes.single()
        assertEquals(mapOf("720p" to "a", "2160p" to "b", "abc" to "c"), episode.sources["en"])
    }

    @Test
    fun `broken rendition is dropped but the episode survives`() {
        val episode = parse(
            """
            {"showId":"1","episodes":[{"number":1,"sources":{
              "en":{"720p":"ok","1080p":null,"480p":{"url":"x"},"360p":42,"240p":""},
              "ar":"not-an-object",
              "hi":{}
            }}]}
            """,
        ).episodes.single()
        assertEquals(mapOf("en" to mapOf("720p" to "ok")), episode.sources)
    }

    @Test
    fun `broken episode is dropped, the rest keep array order and original position`() {
        val episodes = parse(
            """{"showId":"1","episodes":[{"number":1},{"number":"one"},{"number":5},{"number":3}]}""",
        ).episodes
        assertEquals(listOf(1, 5, 3), episodes.map { it.number })
        // index là khoá của watch_progress: server sửa phần tử hỏng thì tập sau không bị đổi định danh.
        assertEquals(listOf(0, 2, 3), episodes.map { it.index })
    }

    @Test
    fun `number is a label only - missing number falls back to position`() {
        val episodes = parse("""{"showId":"1","episodes":[{"number":7},{"number":"x"},{}]}""").episodes
        assertEquals(listOf(7, 3), episodes.map { it.displayNumber })
    }

    @Test
    fun `missing fields get safe defaults - missing isFree means locked`() {
        val episode = parse("""{"showId":"1","episodes":[{"number":1}]}""").episodes.single()
        assertFalse(episode.isFree)
        assertEquals(0L, episode.likeCount)
        assertEquals(emptyMap<String, Map<String, String>>(), episode.sources)
    }

    @Test
    fun `explicit null fields fall back to defaults instead of dropping the episode`() {
        val episode = parse(
            """{"showId":"1","episodes":[{"number":1,"isFree":null,"durationSeconds":null,"likeCount":null,"sources":null}]}""",
        ).episodes.single()
        assertFalse(episode.isFree)
        assertEquals(0, episode.durationSeconds)
        assertEquals(0L, episode.likeCount)
    }

    @Test
    fun `likeCount is Long`() {
        val episode = parse("""{"showId":"1","episodes":[{"likeCount":3000000000}]}""").episodes.single()
        assertEquals(3_000_000_000L, episode.likeCount)
    }
}
