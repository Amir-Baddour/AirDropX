package org.example.Core.Task.Model;

import com.google.gson.JsonObject;

public record AirdropTask(
        String id,
        String airdropId,
        TaskType type,
        String title,
        String description,
        JsonObject config,
        int position
) {
}
