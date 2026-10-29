package com.example.cartly;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// Runs on the computer, no phone needed: Android Studio → right click → Run 'CatalogTest'.
public class CatalogTest {

    private static void assertDept(Catalog.Department expected, String name) {
        assertEquals(name, expected, Catalog.classify(name).department);
    }

    @Test
    public void commonProductsGoToTheirDepartment() {
        assertDept(Catalog.Department.DAIRY, "Latte");
        assertDept(Catalog.Department.DAIRY, "latte parzialmente scremato");
        assertDept(Catalog.Department.DAIRY, "Uova");
        assertDept(Catalog.Department.PRODUCE, "Mele");
        assertDept(Catalog.Department.PRODUCE, "pomodorini");
        assertDept(Catalog.Department.BAKERY, "Pane integrale");
        assertDept(Catalog.Department.PASTA, "Spaghetti n.5");
        assertDept(Catalog.Department.PANTRY, "Caffè");
        assertDept(Catalog.Department.DRINKS, "Acqua frizzante");
        assertDept(Catalog.Department.HOME, "Carta igienica");
        assertDept(Catalog.Department.PERSONAL, "Dentifricio");
        assertDept(Catalog.Department.PETS, "Crocchette per gatti");
        assertDept(Catalog.Department.OTHER, "Regalo per Anna");
    }

    @Test
    public void specificEntriesWinOverGenericOnes() {
        assertDept(Catalog.Department.PANTRY, "Patatine");      // not potatoes
        assertDept(Catalog.Department.DRINKS, "Aranciata");     // not oranges
        assertDept(Catalog.Department.FROZEN, "Spinaci surgelati");
        assertDept(Catalog.Department.FROZEN, "gelato al pistacchio");
        assertDept(Catalog.Department.MEAT_FISH, "Pesce spada"); // not peaches
        assertDept(Catalog.Department.PRODUCE, "Pesche");
        assertDept(Catalog.Department.PRODUCE, "Lattuga");      // not milk
        assertDept(Catalog.Department.DRINKS, "Latte di soia");
    }

    @Test
    public void wholeWordsOnlyWhereAPrefixWouldBeWrong() {
        assertDept(Catalog.Department.PANTRY, "Tè verde");
        assertDept(Catalog.Department.OTHER, "Tegame");        // "te " must not match
        assertDept(Catalog.Department.PANTRY, "sale grosso");
        assertDept(Catalog.Department.OTHER, "Salerno souvenir");
    }

    @Test
    public void emojiFollowsTheProduct() {
        assertEquals("🥛", Catalog.classify("Latte").emoji);
        assertEquals("🍌", Catalog.classify("Banane").emoji);
        assertEquals("🛒", Catalog.classify("qualcosa").emoji);
    }

    @Test
    public void quantityIsReadFromTheTypedText() {
        assertEntry("Latte", 2, "2 latte");
        assertEntry("Latte", 2, "2x latte");
        assertEntry("Latte", 3, "latte x3");
        assertEntry("Uova", 6, "uova 6");
        assertEntry("Pasta 500", 1, "Pasta 500");             // 500 is grams, not a quantity
        assertEntry("Acqua 2 litri", 1, "acqua 2 litri");
        assertEntry("7up", 1, "7up");
    }

    private static void assertEntry(String name, int quantity, String typed) {
        Ui.Entry entry = Ui.parseEntry(typed);
        assertEquals(typed, name, entry.name);
        assertEquals(typed, quantity, entry.quantity);
    }
}
