package org.example.Api.Airdrop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.Core.Airdrop.Model.RecipientInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AirdropApiParseTest {

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void parsesStringAndNumberAmounts() {
        List<RecipientInput> list = AirdropApi.parseRecipients(json(
                "{\"recipients\":[{\"address\":\"0xa\",\"amount\":\"10.5\"},{\"address\":\"0xb\",\"amount\":3}]}"));
        assertEquals(2, list.size());
        assertEquals(new BigDecimal("10.5"), list.get(0).amount());
        assertEquals(0, new BigDecimal("3").compareTo(list.get(1).amount()));
    }

    @Test
    void missingAmountBecomesNullForTheValidator() {
        List<RecipientInput> list = AirdropApi.parseRecipients(json("{\"recipients\":[{\"address\":\"0xa\"}]}"));
        assertNull(list.get(0).amount());
    }

    @Test
    void rejectsMissingArrayAndBadNumbers() {
        assertThrows(IllegalArgumentException.class, () -> AirdropApi.parseRecipients(json("{}")));
        assertThrows(IllegalArgumentException.class, () -> AirdropApi.parseRecipients(json("{\"recipients\":\"x\"}")));
        assertThrows(IllegalArgumentException.class, () -> AirdropApi.parseRecipients(json(
                "{\"recipients\":[{\"address\":\"0xa\",\"amount\":\"ten\"}]}")));
        assertThrows(IllegalArgumentException.class, () -> AirdropApi.parseRecipients(json("{\"recipients\":[1]}")));
    }
}
