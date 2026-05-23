package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val TEXT_MODELS = stringPreferencesKey("text_models")
        val IMAGE_MODELS = stringPreferencesKey("image_models")
        val TEXT_SYSTEM_PROMPT = stringPreferencesKey("text_system_prompt")
        val IMAGE_SYSTEM_PROMPT = stringPreferencesKey("image_system_prompt")
        val FONT_SIZE = floatPreferencesKey("font_size")

        const val DEFAULT_TEXT_MODELS = "openai,mistral,mistral-large,llama"
        const val DEFAULT_IMAGE_MODELS = "flux,zimage,qwen-image,gptimage-large,wan-image"
        const val DEFAULT_TEXT_SYSTEM_PROMPT = "Bạn là trợ lý AI thông minh."
        const val DEFAULT_IMAGE_SYSTEM_PROMPT = """Bạn là chuyên gia viết prompt tạo ảnh. Hãy nhận ý tưởng và mở rộng thành prompt CHI TIẾT BẰNG TIẾNG ANH.
Hướng dẫn cho model:
- flux: Prompt tự nhiên, chi tiết ánh sáng, phong cách nhiếp ảnh/hội họa.
- zimage: Bố cục, concept art, màu sặc sỡ, digital art.
- qwen-image: Mô tả rõ đối tượng, bối cảnh, độ tương phản.
- gptimage-large: Phong cách điện ảnh, siêu thực, 8k resolution, ống kính máy ảnh.
- wan-image: Nghệ thuật hoặc anime, manga detail.
QUAN TRỌNG: Chỉ trả về nguyên văn đoạn prompt bằng tiếng Anh, KHÔNG giải thích, KHÔNG có text thừa."""
        const val DEFAULT_FONT_SIZE = 16f
    }

    val textModelsFlow: Flow<String> = context.dataStore.data.map { it[TEXT_MODELS] ?: DEFAULT_TEXT_MODELS }
    val imageModelsFlow: Flow<String> = context.dataStore.data.map { it[IMAGE_MODELS] ?: DEFAULT_IMAGE_MODELS }
    val textSystemPromptFlow: Flow<String> = context.dataStore.data.map { it[TEXT_SYSTEM_PROMPT] ?: DEFAULT_TEXT_SYSTEM_PROMPT }
    val imageSystemPromptFlow: Flow<String> = context.dataStore.data.map { it[IMAGE_SYSTEM_PROMPT] ?: DEFAULT_IMAGE_SYSTEM_PROMPT }
    val fontSizeFlow: Flow<Float> = context.dataStore.data.map { it[FONT_SIZE] ?: DEFAULT_FONT_SIZE }

    suspend fun saveValues(
        textModels: String,
        imageModels: String,
        textSystemPrompt: String,
        imageSystemPrompt: String,
        fontSize: Float
    ) {
        context.dataStore.edit { prefs ->
            prefs[TEXT_MODELS] = textModels
            prefs[IMAGE_MODELS] = imageModels
            prefs[TEXT_SYSTEM_PROMPT] = textSystemPrompt
            prefs[IMAGE_SYSTEM_PROMPT] = imageSystemPrompt
            prefs[FONT_SIZE] = fontSize
        }
    }
}
