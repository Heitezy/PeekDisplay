package heitezy.peekdisplay.actions.alwayson.draw

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import androidx.core.content.ContextCompat
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.services.NotificationService
import kotlin.math.min
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap

object NotificationPreview {
    const val DISMISS_ACTION_INDEX = -2
    
    private const val PREVIEW_WIDTH_PERCENT = 0.9f
    private const val PREVIEW_MAX_HEIGHT = 400
    private const val CORNER_RADIUS = 16f
    private const val PADDING = 16
    private const val TITLE_TEXT_SIZE = 16f
    private const val BODY_TEXT_SIZE = 14f
    private const val ACTION_TEXT_SIZE = 14f
    private const val ACTION_BUTTON_HEIGHT = 96
    private const val ACTION_BUTTON_RADIUS = 8f
    private const val APP_ICON_SIZE = 24f
    private const val AVATAR_SIZE = 30f
    private const val REPLY_TEXT_SIZE = 14f
    private const val REPLY_FIELD_HEIGHT = 72
    private const val SEND_BUTTON_SIZE = 72f

    internal var currentNotificationIndex: Int? = null
    private var previewRect: RectF? = null
    private var actionButtons = mutableListOf<Rect>()
    private var dismissButtonRect: Rect? = null
    
    // Reply functionality
    private var isReplyActive = false
    private var replyAction: Notification.Action? = null
    private var replyText = ""
    private var replyFieldRect = Rect()
    private var sendButtonRect = Rect()

    private val backgroundPaint = Paint().apply {
        color = "#BB000000".toColorInt() // Semi-transparent black
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = TITLE_TEXT_SIZE * 3
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private val appNamePaint = Paint().apply {
        color = "#E6FFFFFF".toColorInt() // Slightly transparent white (90%)
        textSize = TITLE_TEXT_SIZE * 2.5f
        isAntiAlias = true
    }

    private val timestampPaint = Paint().apply {
        color = "#E6FFFFFF".toColorInt() // Slightly transparent white (90%)
        textSize = TITLE_TEXT_SIZE * 2.5f
        isAntiAlias = true
    }

    private val bodyPaint = Paint().apply {
        color = Color.WHITE
        textSize = BODY_TEXT_SIZE * 3
        isAntiAlias = true
    }

    private val actionBackgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val actionTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = ACTION_TEXT_SIZE * 3
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val dismissButtonPaint = Paint().apply {
        color = "#F44336".toColorInt() // Red color for dismiss
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val dismissTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = ACTION_TEXT_SIZE * 3
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val iconPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    // Reply UI elements
    private val replyFieldPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val replyTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = REPLY_TEXT_SIZE * 3
        isAntiAlias = true
    }
    
    private val sendButtonPaint = Paint().apply {
        color = "#4285F4".toColorInt()  // Google blue color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Set the current notification to preview
    fun setCurrentNotification(index: Int?) {
        currentNotificationIndex = index
        actionButtons.clear()
        dismissButtonRect = null
        // Clear reply state when changing notifications
        isReplyActive = false
        replyAction = null
        replyText = ""
    }
    
    fun getCurrentNotification(): Int? = currentNotificationIndex
    
    fun activateReplyMode(action: Notification.Action) {
        isReplyActive = true
        replyAction = action
        replyText = ""
    }
    
    fun isReplyActive(): Boolean = isReplyActive
    
    fun appendCharToReply(c: Char) {
        replyText += c
    }
    
    fun deleteLastCharFromReply() {
        if (replyText.isNotEmpty()) {
            replyText = replyText.substring(0, replyText.length - 1)
        }
    }

    fun clearReplyMode() {
        isReplyActive = false
        replyAction = null
        replyText = ""
    }
    
    fun isPointInReplyField(x: Float, y: Float): Boolean {
        return replyFieldRect.contains(x.toInt(), y.toInt())
    }
    
    fun isPointInSendButton(x: Float, y: Float): Boolean {
        return sendButtonRect.contains(x.toInt(), y.toInt())
    }

    fun isPointInAction(x: Float, y: Float): Int {
        // If in reply mode, don't detect regular action buttons
        if (isReplyActive) return -1
        
        // Check if the point is in the dismiss button
        if (dismissButtonRect?.contains(x.toInt(), y.toInt()) == true) {
            return DISMISS_ACTION_INDEX
        }
        
        for ((index, rect) in actionButtons.withIndex()) {
            if (rect.contains(x.toInt(), y.toInt())) {
                return index
            }
        }
        return -1
    }

    fun isPointInBodyArea(x: Float, y: Float): Boolean {
        val rect = previewRect ?: return false
        val bodyAreaTop = rect.top + PADDING + titlePaint.textSize + PADDING
        val bodyAreaBottom = if (actionButtons.isNotEmpty() || dismissButtonRect != null) {
            (actionButtons.firstOrNull()?.top ?: dismissButtonRect?.top)?.toFloat()
                ?: (rect.bottom - PADDING)
        } else {
            rect.bottom - PADDING
        }
        
        // Expand the body area to account for potentially two lines of body text
        val bodyArea = RectF(rect.left + PADDING, bodyAreaTop, rect.right - PADDING, bodyAreaBottom)
        return bodyArea.contains(x, y)
    }

    fun draw(canvas: Canvas, utils: Utils, width: Int, height: Int) {
        val notificationIndex = currentNotificationIndex ?: return
        val notification = NotificationService.notifications.getOrNull(notificationIndex) ?: return
        val detailed = NotificationService.detailed

        val previewWidth = width * PREVIEW_WIDTH_PERCENT
        val previewHeight = min(PREVIEW_MAX_HEIGHT, height / 2)

        val iconBounds = NotificationIcons.iconBounds[notificationIndex]
        if (iconBounds == null) {
            currentNotificationIndex = null
            return
        }
        
        val previewLeft = (width - previewWidth) / 2
        
        // Check the notification preview position setting
        val previewPosition = utils.prefs.get(P.NOTIFICATION_PREVIEW_POSITION, P.NOTIFICATION_PREVIEW_POSITION_DEFAULT)
        val previewTop = if (previewPosition == "above") {
            // Position preview above the icon
            iconBounds.top - previewHeight - PADDING * 2f
        } else {
            // Position preview below the icon (default behavior)
            iconBounds.bottom + PADDING * 2f
        }
        
        val previewRight = previewLeft + previewWidth
        val previewBottom = previewTop + previewHeight
        
        val rect = RectF(previewLeft, previewTop, previewRight, previewBottom)
        previewRect = rect

        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, backgroundPaint)

        val detailedNotification = detailed.find { it.packageName == notification.packageName }

        var title = ""
        var body = ""
        val actions = mutableListOf<Notification.Action>()
        
        if (detailedNotification != null) {
            val extras = detailedNotification.notification.extras
            title = extras.getCharSequence(Notification.EXTRA_TITLE, "").toString()
            body = extras.getCharSequence(Notification.EXTRA_TEXT, "").toString()

            detailedNotification.notification.actions?.let { notificationActions ->
                actions.addAll(notificationActions)
            }
        }

        val appName = try {
            utils.context.packageManager.getApplicationLabel(
                utils.context.packageManager.getApplicationInfo(notification.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            notification.packageName
        }

        val appIcon = try {
            utils.context.packageManager.getApplicationIcon(notification.packageName)
        } catch (e: Exception) {
            null
        }

        var avatarBitmap: Bitmap? = null
        if (detailedNotification != null) {
            val extras = detailedNotification.notification.extras
            val largeIcon = extras.getParcelable<Icon>(Notification.EXTRA_LARGE_ICON)
            if (largeIcon != null) {
                try {
                    avatarBitmap = largeIcon.loadDrawable(utils.context)?.toBitmap()
                } catch (e: Exception) {
                    // Fallback if converting to bitmap fails
                }
            }
        }

        val timestamp = detailedNotification?.postTime ?: System.currentTimeMillis()
        val formattedTime = getRelativeTimeSpanString(timestamp)

        val appNameY = previewTop + PADDING + appNamePaint.textSize / 2
        val titleY = appNameY + PADDING + titlePaint.textSize
        var bodyY = titleY + PADDING + bodyPaint.textSize

        val iconSize = APP_ICON_SIZE * 3 // Scale up for high resolution
        val iconY = appNameY - iconSize / 3 * 2
        val iconX = previewLeft + PADDING
        if (appIcon != null) {
            appIcon.setBounds(
                iconX.toInt(),
                iconY.toInt(),
                (iconX + iconSize).toInt(),
                (iconY + iconSize).toInt()
            )
            appIcon.draw(canvas)
        }

        canvas.drawText(appName, iconX + iconSize + PADDING, appNameY, appNamePaint)

        val appNameWidth = appNamePaint.measureText(appName)
        val timestampX = iconX + iconSize + PADDING + appNameWidth
        canvas.drawText(" • $formattedTime", timestampX, appNameY, timestampPaint)

        val avatarSize = AVATAR_SIZE * 3 // Scale up for high resolution
        if (avatarBitmap != null) {
            val avatarX = previewRight - PADDING - avatarSize
            val avatarY = appNameY - avatarSize / 2

            val avatarRect = RectF(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize)
            canvas.drawOval(avatarRect, backgroundPaint) // Draw background circle
            
            // Scale and draw the bitmap
            val src = Rect(0, 0, avatarBitmap.width, avatarBitmap.height)
            val dst = Rect(avatarX.toInt(), avatarY.toInt(), 
                          (avatarX + avatarSize).toInt(), (avatarY + avatarSize).toInt())
            canvas.drawBitmap(avatarBitmap, src, dst, iconPaint)
        }

        if (title.isNotEmpty()) {
            // Wrap title text to fit within available width
            val wrappedTitle = wrapText(title, titlePaint, previewWidth)
            if (wrappedTitle.isNotEmpty()) {
                canvas.drawText(wrappedTitle[0], previewLeft + PADDING, titleY, titlePaint)
            }
        } else {
            bodyY = titleY
        }

        if (body.isNotEmpty()) {
            // Wrap body text to fit within available width
            val wrappedBody = wrapText(body, bodyPaint, previewWidth)
            if (wrappedBody.isNotEmpty()) {
                canvas.drawText(wrappedBody[0], previewLeft + PADDING, bodyY, bodyPaint)
                
                // Draw second line of body text if available
                if (wrappedBody.size > 1) {
                    val secondLineY = bodyY + bodyPaint.textSize + PADDING / 2
                    canvas.drawText(wrappedBody[1], previewLeft + PADDING, secondLineY, bodyPaint)
                }
            }
        }

        // Check if in reply mode
        if (isReplyActive) {
            // Draw reply UI at the bottom of the preview
            val replyY = previewBottom - PADDING - REPLY_FIELD_HEIGHT
            
            // Draw reply text field
            val replyFieldLeft = previewLeft + PADDING
            val replyFieldRight = previewRight - PADDING - SEND_BUTTON_SIZE - PADDING
            val replyFieldRect = RectF(
                replyFieldLeft, 
                replyY, 
                replyFieldRight, 
                replyY + REPLY_FIELD_HEIGHT
            )
            canvas.drawRoundRect(replyFieldRect, ACTION_BUTTON_RADIUS, ACTION_BUTTON_RADIUS, replyFieldPaint)
            
            // Store the rect for touch detection
            this.replyFieldRect = Rect(
                replyFieldLeft.toInt(),
                replyY.toInt(),
                replyFieldRight.toInt(),
                (replyY + REPLY_FIELD_HEIGHT).toInt()
            )
            
            // Draw reply text
            val replyTextY = replyY + (REPLY_FIELD_HEIGHT + replyTextPaint.textSize) / 2 - PADDING / 2
            val displayText = if (replyText.isEmpty()) "Type a reply..." else replyText
            val textPaint = if (replyText.isEmpty()) {
                // Use gray for hint text
                Paint(replyTextPaint).apply { color = Color.GRAY }
            } else {
                replyTextPaint
            }
            canvas.drawText(displayText, replyFieldLeft + PADDING, replyTextY, textPaint)
            
            // Draw send button
            val sendButtonLeft = replyFieldRight + PADDING
            val sendButtonRect = RectF(
                sendButtonLeft,
                replyY,
                sendButtonLeft + SEND_BUTTON_SIZE,
                replyY + REPLY_FIELD_HEIGHT
            )
            canvas.drawRoundRect(sendButtonRect, ACTION_BUTTON_RADIUS, ACTION_BUTTON_RADIUS, sendButtonPaint)
            
            // Store the send button rect for touch detection
            this.sendButtonRect = Rect(
                sendButtonLeft.toInt(),
                replyY.toInt(),
                (sendButtonLeft + SEND_BUTTON_SIZE).toInt(),
                (replyY + REPLY_FIELD_HEIGHT).toInt()
            )
            
            // Draw send icon instead of text
            val sendIcon = ContextCompat.getDrawable(utils.context, R.drawable.ic_send)
            if (sendIcon != null) {
                val iconPadding = PADDING / 2
                sendIcon.setBounds(
                    (sendButtonLeft + iconPadding).toInt(),
                    (replyY + iconPadding).toInt(),
                    (sendButtonLeft + SEND_BUTTON_SIZE - iconPadding).toInt(),
                    (replyY + REPLY_FIELD_HEIGHT - iconPadding).toInt()
                )
                sendIcon.draw(canvas)
            }
            
        } else {
            // Draw regular action buttons
            actionButtons.clear()
            
            // Calculate space for action buttons and dismiss button
            val totalButtons = min(actions.size, 3) + 1 // +1 for dismiss button
            val buttonWidth = (previewWidth - (PADDING * 2)) / totalButtons
            val actionY = previewBottom - PADDING - ACTION_BUTTON_HEIGHT
            
            // Draw the dismiss button first
            val dismissLeft = previewLeft + PADDING
            val dismissTop = actionY
            val dismissRight = dismissLeft + buttonWidth - PADDING
            val dismissBottom = dismissTop + ACTION_BUTTON_HEIGHT
            
            val dismissRect = RectF(dismissLeft, dismissTop, dismissRight, dismissBottom)
            canvas.drawRoundRect(dismissRect, ACTION_BUTTON_RADIUS, ACTION_BUTTON_RADIUS, dismissButtonPaint)
            
            // Draw dismiss text
            val dismissTextX = (dismissLeft + dismissRight) / 2
            val dismissText = utils.context.getString(R.string.notification_dismiss)
            
            // Wrap text to fit button width (for consistency with action buttons)
            val dismissButtonWidth = dismissRight - dismissLeft - (2 * PADDING)
            val wrappedDismissText = wrapText(dismissText, dismissTextPaint, dismissButtonWidth)
            
            if (wrappedDismissText.isNotEmpty()) {
                // Calculate vertical positioning for text lines
                val lineHeight = dismissTextPaint.textSize * 1.2f
                val totalTextHeight = lineHeight * wrappedDismissText.size
                var yPosition = dismissTop + (ACTION_BUTTON_HEIGHT - totalTextHeight) / 2 + dismissTextPaint.textSize
                
                // Draw each line of text
                for (line in wrappedDismissText) {
                    canvas.drawText(line, dismissTextX, yPosition, dismissTextPaint)
                    yPosition += lineHeight
                }
            }
            
            // Store dismiss button bounds for touch detection
            dismissButtonRect = Rect(
                dismissLeft.toInt(), 
                dismissTop.toInt(), 
                dismissRight.toInt(), 
                dismissBottom.toInt()
            )
            
            // Draw other action buttons
            for (i in 0 until min(actions.size, 3)) {
                val action = actions[i]
                val actionLeft = dismissRight + PADDING + (i * buttonWidth)
                val actionTop = actionY
                val actionRight = actionLeft + buttonWidth - PADDING
                val actionBottom = actionTop + ACTION_BUTTON_HEIGHT
                
                val actionRect = RectF(actionLeft, actionTop, actionRight, actionBottom)
                
                // Draw action button background (white)
                canvas.drawRoundRect(actionRect, ACTION_BUTTON_RADIUS, ACTION_BUTTON_RADIUS, actionBackgroundPaint)
                
                // Draw action text (black)
                val actionTitle = action.title?.toString() ?: ""
                val textX = (actionLeft + actionRight) / 2
                
                // Wrap text to fit button width
                val buttonWidth = actionRight - actionLeft - (2 * PADDING)
                val wrappedText = wrapText(actionTitle, actionTextPaint, buttonWidth)
                
                if (wrappedText.isEmpty()) {
                    // Handle empty action title
                    actionButtons.add(Rect(actionLeft.toInt(), actionTop.toInt(), actionRight.toInt(), actionBottom.toInt()))
                    continue
                }
                
                // Calculate vertical positioning for text lines
                val lineHeight = actionTextPaint.textSize * 1.2f
                val totalTextHeight = lineHeight * wrappedText.size
                var yPosition = actionTop + (ACTION_BUTTON_HEIGHT - totalTextHeight) / 2 + actionTextPaint.textSize
                
                // Draw each line of text
                for (line in wrappedText) {
                    canvas.drawText(line, textX, yPosition, actionTextPaint)
                    yPosition += lineHeight
                }
                
                // Store action button bounds for touch detection
                actionButtons.add(Rect(actionLeft.toInt(), actionTop.toInt(), actionRight.toInt(), actionBottom.toInt()))
            }
        }
    }

    private fun getRelativeTimeSpanString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        
        return DateUtils.getRelativeTimeSpanString(
            timestamp,
            now,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    // Function to wrap text to fit within a specified width
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf()
        
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val testWidth = paint.measureText(testLine)
            
            if (testWidth <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    // If a single word is too long, add it anyway
                    lines.add(word)
                    currentLine = StringBuilder()
                }
            }
        }
        
        // Add the last line if there's any content
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        
        // Limit to 2 lines maximum
        return if (lines.size > 2) {
            val combinedLines = lines.subList(0, 2).toMutableList()
            val lastLine = combinedLines[1]
            if (lastLine.length > 4) {
                combinedLines[1] = lastLine.substring(0, lastLine.length - 3) + "..."
            }
            combinedLines
        } else {
            lines
        }
    }

    private fun Drawable.toBitmap(): Bitmap? {
        try {
            val bitmap =
                createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1))
            
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            return bitmap
        } catch (e: Exception) {
            return null
        }
    }

    fun sendReply(context: Context): Boolean {
        val action = replyAction ?: return false
        
        // Don't send empty replies
        if (replyText.isEmpty()) {
            return false
        }
        
        try {
            // Find RemoteInput for this action
            val remoteInputs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                action.remoteInputs
            } else {
                action.remoteInputs
            }
            
            if (remoteInputs != null && remoteInputs.isNotEmpty()) {
                val remoteInput = remoteInputs[0]
                
                // Create a bundle with the reply text
                val resultBundle = Bundle()
                resultBundle.putCharSequence(remoteInput.resultKey, replyText)
                
                // Create the intent
                val intent = Intent()
                RemoteInput.addResultsToIntent(remoteInputs, intent, resultBundle)
                
                // Send the pending intent with the reply
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34 (Android 14)
                    val options = android.app.ActivityOptions.makeBasic()
                    options.setPendingIntentBackgroundActivityStartMode(
                        android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                    action.actionIntent.send(
                        context, 0, intent, null, null, null,
                        options.toBundle()
                    )
                } else {
                    action.actionIntent.send(context, 0, intent)
                }
                
                // Clear reply mode after sending
                clearReplyMode()
                return true
            }
        } catch (e: Exception) {
            Log.e(Global.LOG_TAG, "Failed to send reply: ${e.message}")
        }
        
        return false
    }
} 
