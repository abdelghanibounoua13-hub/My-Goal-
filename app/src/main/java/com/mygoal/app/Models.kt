package com.mygoal.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.json.*
import java.util.*

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String, val description: String = "", val icon: String = "Star",
    val color: Long = 0xFFE6EEF8, val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notebook_pages", foreignKeys = [ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)], indices = [Index("goalId")])
data class NotebookPageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String, val title: String, val content: String = "", val plainText: String = "",
    val pageOrder: Int = 0, val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface MyGoalDao {
    @Query("SELECT * FROM goals ORDER BY updatedAt DESC") fun getAllGoals(): Flow<List<GoalEntity>>
    @Query("SELECT * FROM notebook_pages ORDER BY pageOrder ASC, updatedAt DESC") fun getAllPages(): Flow<List<NotebookPageEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveGoal(goal: GoalEntity)
    @Delete suspend fun deleteGoal(goal: GoalEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePage(page: NotebookPageEntity)
    @Delete suspend fun deletePage(page: NotebookPageEntity)
    @Query("DELETE FROM goals") suspend fun clearAllGoals()
}

@Database(entities = [GoalEntity::class, NotebookPageEntity::class], version = 3, exportSchema = false)
abstract class MyGoalDatabase : RoomDatabase() { abstract fun dao(): MyGoalDao }

val drawerPalette = listOf(
    Triple(0xFFE6EEF8, "أزرق ضبابي", "Mist Blue"), Triple(0xFFEDE8F8, "لافندر ناعم", "Lavender"),
    Triple(0xFFE6F4EA, "أخضر مريمي", "Sage Mint"), Triple(0xFFFFF2DE, "كهرماني دافئ", "Warm Amber"),
    Triple(0xFFFCE8E8, "وردي هادئ", "Blush Rose"), Triple(0xFFE3F4F4, "تركواز هادئ", "Calm Teal"),
    Triple(0xFFF4EFE6, "رملي دافئ", "Sandstone"), Triple(0xFFEFE8F4, "أوركيد فاتح", "Orchid")
)

val drawerIcons = linkedMapOf(
    "Star" to Icons.Default.Star, "Book" to Icons.Default.MenuBook, "Language" to Icons.Default.Language,
    "Fitness" to Icons.Default.FitnessCenter, "Business" to Icons.Default.Work, "Code" to Icons.Default.Code,
    "Lightbulb" to Icons.Default.Lightbulb, "School" to Icons.Default.School, "Brush" to Icons.Default.Brush,
    "Savings" to Icons.Default.Savings, "Explore" to Icons.Default.Explore, "Psychology" to Icons.Default.Psychology,
    "Music" to Icons.Default.MusicNote, "Rocket" to Icons.Default.RocketLaunch, "Heart" to Icons.Default.Favorite,
    "Trophy" to Icons.Default.EmojiEvents
)

data class PageBlock(val id: String = UUID.randomUUID().toString(), val type: String = "PARAGRAPH", val text: String = "", val checked: Boolean = false, val extra: String = "")

object Codec {
    fun decode(raw: String): List<PageBlock> = runCatching {
        if (raw.isBlank()) return listOf(PageBlock())
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            PageBlock(o.optString("id", UUID.randomUUID().toString()), o.optString("type", "PARAGRAPH"), o.optString("text"), o.optBoolean("checked"), o.optString("extra"))
        }.ifEmpty { listOf(PageBlock()) }
    }.getOrElse { listOf(PageBlock(text = raw)) }

    fun encode(list: List<PageBlock>): String = JSONArray().apply {
        list.forEach { b -> put(JSONObject().apply { put("id", b.id); put("type", b.type); put("text", b.text); put("checked", b.checked); put("extra", b.extra) }) }
    }.toString()

    fun plain(list: List<PageBlock>) = list.joinToString("\n") { "${it.text} ${it.extra}" }.trim()

    fun decodeTable(s: String): List<List<String>> = runCatching {
        val r = JSONArray(s)
        List(r.length()) { i -> val c = r.getJSONArray(i); List(c.length()) { j -> c.optString(j) } }
    }.getOrElse { listOf(listOf("البند", "التفاصيل"), listOf("", "")) }

    fun encodeTable(g: List<List<String>>): String = JSONArray().apply { g.forEach { r -> put(JSONArray(r)) } }.toString()
}

class RichTransform : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text; val b = AnnotatedString.Builder(s); val dim = SpanStyle(color = Color.Gray.copy(alpha = 0.4f))
        Regex("\\*\\*(.+?)\\*\\*").findAll(s).forEach { m -> b.addStyle(SpanStyle(fontWeight = FontWeight.Black), m.range.first, m.range.last + 1); b.addStyle(dim, m.range.first, m.range.first + 2); b.addStyle(dim, m.range.last - 1, m.range.last + 1) }
        Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)").findAll(s).forEach { m -> b.addStyle(SpanStyle(fontStyle = FontStyle.Italic), m.range.first, m.range.last + 1) }
        Regex("__(.+?)__").findAll(s).forEach { m -> b.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), m.range.first, m.range.last + 1) }
        Regex("==(.+?)==").findAll(s).forEach { m -> b.addStyle(SpanStyle(background = Color(0xFFFFF099), color = Color(0xFF1B2A4A)), m.range.first, m.range.last + 1) }
        Regex("\\[(.+?)]\\((.+?)\\)").findAll(s).forEach { m -> b.addStyle(SpanStyle(color = Color(0xFF3D5AFE), textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1) }
        return TransformedText(b.toAnnotatedString(), OffsetMapping.Identity)
    }
}
