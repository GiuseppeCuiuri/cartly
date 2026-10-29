package com.example.cartly;

import androidx.annotation.StringRes;

import java.text.Normalizer;
import java.util.Locale;

// Guesses the supermarket department and an emoji from a product's name: "2 litri di latte" → Latticini, 🥛.
// Only emoji up to Emoji 11 (Android 9, the minSdk), so no product shows an empty box.
final class Catalog {

    /** In the order of a walk through a typical Italian supermarket. */
    enum Department {
        PRODUCE(R.string.dept_produce, "🥬"),
        BAKERY(R.string.dept_bakery, "🥖"),
        DAIRY(R.string.dept_dairy, "🧀"),
        MEAT_FISH(R.string.dept_meat_fish, "🥩"),
        PASTA(R.string.dept_pasta, "🍝"),
        PANTRY(R.string.dept_pantry, "🥫"),
        FROZEN(R.string.dept_frozen, "❄️"),
        DRINKS(R.string.dept_drinks, "🥤"),
        HOME(R.string.dept_home, "🧽"),
        PERSONAL(R.string.dept_personal, "🧴"),
        PETS(R.string.dept_pets, "🐾"),
        OTHER(R.string.dept_other, "🛒");

        @StringRes
        final int label;
        final String emoji;

        Department(@StringRes int label, String emoji) {
            this.label = label;
            this.emoji = emoji;
        }
    }

    static final class Match {
        final Department department;
        final String emoji;

        Match(Department department, String emoji) {
            this.department = department;
            this.emoji = emoji;
        }
    }

    private static final Object[][] KEYWORDS;

    static {
        Department P = Department.PRODUCE, B = Department.BAKERY, D = Department.DAIRY, M = Department.MEAT_FISH,
                S = Department.PASTA, C = Department.PANTRY, F = Department.FROZEN, W = Department.DRINKS,
                H = Department.HOME, X = Department.PERSONAL, A = Department.PETS;
        // {word start, emoji, department}; a trailing space means a whole word. First match wins, so the
        // more specific entries ("patatine", "aranciata", "carta igienica") come before the generic ones.
        KEYWORDS = new Object[][]{
                {"surgelat", "❄️", F}, {"gelat", "🍦", F}, {"bastoncin", "🐟", F}, {"ghiaccio", "❄️", F},
                {"patatine", "🍟", C}, {"fette biscottate", "🍞", C}, {"aranciata", "🥤", W}, {"te freddo", "🥤", W},
                {"the freddo", "🥤", W}, {"carta igienica", "🧻", H}, {"carta cucina", "🧻", H}, {"carta da cucina", "🧻", H},
                {"carta forno", "📦", H}, {"cibo per gatti", "🐱", A}, {"cibo per cani", "🐶", A}, {"latte di", "🥛", W},
                {"tonno", "🐟", C}, {"sgombro", "🐟", C}, {"crema spalmabile", "🍫", C},

                {"mela ", "🍎", P}, {"mele ", "🍎", P}, {"pera ", "🍐", P}, {"pere ", "🍐", P}, {"banan", "🍌", P},
                {"aranc", "🍊", P}, {"mandarin", "🍊", P}, {"clementin", "🍊", P}, {"limon", "🍋", P}, {"fragol", "🍓", P},
                {"uva ", "🍇", P}, {"kiwi", "🥝", P}, {"anguri", "🍉", P}, {"melone", "🍈", P}, {"meloni", "🍈", P},
                {"pesca ", "🍑", P}, {"pesche", "🍑", P}, {"albicocc", "🍑", P}, {"ananas", "🍍", P}, {"avocad", "🥑", P},
                {"cocco", "🥥", P}, {"cilieg", "🍒", P}, {"frutta", "🍎", P}, {"pomodor", "🍅", P}, {"insalat", "🥬", P},
                {"lattug", "🥬", P}, {"rucol", "🥬", P}, {"spinac", "🥬", P}, {"bietol", "🥬", P}, {"cavol", "🥦", P},
                {"brocc", "🥦", P}, {"verza", "🥬", P}, {"carot", "🥕", P}, {"patat", "🥔", P}, {"cipoll", "🥬", P},
                {"aglio", "🥬", P}, {"porri", "🥬", P}, {"sedano", "🥬", P}, {"zucchin", "🥒", P}, {"cetriol", "🥒", P},
                {"melanzan", "🍆", P}, {"peperon", "🌶️", P}, {"fungh", "🍄", P}, {"fungo", "🍄", P}, {"mais", "🌽", P},
                {"zucca", "🎃", P}, {"basilic", "🌿", P}, {"prezzemol", "🌿", P}, {"rosmarin", "🌿", P}, {"salvia", "🌿", P},
                {"verdur", "🥬", P}, {"fagiolin", "🥬", P},

                {"pane", "🍞", B}, {"panin", "🍞", B}, {"baguette", "🥖", B}, {"focacc", "🍞", B}, {"piadin", "🍞", B},
                {"pizza", "🍕", B}, {"cornett", "🥐", B}, {"brioche", "🥐", B}, {"croissant", "🥐", B}, {"grissin", "🥖", B},
                {"torta", "🍰", B}, {"pasticcin", "🍰", B},

                {"latte", "🥛", D}, {"yogurt", "🥛", D}, {"panna", "🥛", D}, {"burro", "🥛", D}, {"formagg", "🧀", D},
                {"parmigian", "🧀", D}, {"grana", "🧀", D}, {"mozzarell", "🧀", D}, {"ricotta", "🧀", D},
                {"mascarpone", "🧀", D}, {"stracchin", "🧀", D}, {"gorgonzola", "🧀", D}, {"pecorin", "🧀", D},
                {"scamorza", "🧀", D}, {"provola", "🧀", D}, {"fontina", "🧀", D}, {"uova", "🥚", D}, {"uovo", "🥚", D},

                {"pollo", "🍗", M}, {"tacchin", "🍗", M}, {"carne", "🥩", M}, {"manzo", "🥩", M}, {"maiale", "🥩", M},
                {"vitello", "🥩", M}, {"bistecc", "🥩", M}, {"macinat", "🥩", M}, {"fettin", "🥩", M}, {"hamburger", "🍔", M},
                {"salsicc", "🌭", M}, {"wurstel", "🌭", M}, {"prosciutt", "🥓", M}, {"salame", "🥓", M}, {"mortadella", "🥓", M},
                {"speck", "🥓", M}, {"bresaola", "🥓", M}, {"pancetta", "🥓", M}, {"affettat", "🥓", M}, {"pesce", "🐟", M},
                {"pesci", "🐟", M}, {"salmone", "🐟", M}, {"merluzz", "🐟", M}, {"orata", "🐟", M}, {"branzin", "🐟", M},
                {"gamber", "🦐", M}, {"cozze", "🐚", M}, {"vongol", "🐚", M}, {"calamar", "🦑", M}, {"polpo", "🐙", M},

                {"pasta", "🍝", S}, {"spaghett", "🍝", S}, {"penne", "🍝", S}, {"fusilli", "🍝", S}, {"rigaton", "🍝", S},
                {"maccheron", "🍝", S}, {"tagliatell", "🍝", S}, {"lasagn", "🍝", S}, {"gnocchi", "🍝", S}, {"tortellin", "🍝", S},
                {"ravioli", "🍝", S}, {"riso", "🍚", S}, {"farro", "🌾", S}, {"orzo", "🌾", S}, {"cous", "🍚", S},
                {"quinoa", "🌾", S}, {"cereali", "🥣", S}, {"muesli", "🥣", S}, {"fiocchi", "🥣", S}, {"avena", "🥣", S},

                {"farina", "🌾", C}, {"lievito", "🌾", C}, {"zucchero", "🍬", C}, {"sale ", "🧂", C}, {"pepe ", "🧂", C},
                {"spezie", "🧂", C}, {"olio", "🥫", C}, {"aceto", "🥫", C}, {"passata", "🍅", C}, {"pelati", "🍅", C},
                {"sugo", "🍅", C}, {"pesto", "🌿", C}, {"maionese", "🥫", C}, {"ketchup", "🥫", C}, {"senape", "🥫", C},
                {"legumi", "🥫", C}, {"fagioli", "🥫", C}, {"ceci", "🥫", C}, {"lenticchie", "🥫", C}, {"brodo", "🥫", C},
                {"dado", "🥫", C}, {"caffe", "☕", C}, {"te ", "🍵", C}, {"the ", "🍵", C}, {"tisan", "🍵", C},
                {"camomill", "🍵", C}, {"biscott", "🍪", C}, {"cracker", "🍘", C}, {"cioccolat", "🍫", C}, {"nutella", "🍫", C},
                {"cacao", "🍫", C}, {"marmellat", "🍯", C}, {"confettur", "🍯", C}, {"miele", "🍯", C}, {"merendin", "🍪", C},
                {"snack", "🍿", C}, {"pop corn", "🍿", C}, {"noci", "🥜", C}, {"mandorl", "🥜", C}, {"nocciol", "🥜", C},
                {"arachid", "🥜", C}, {"pistacch", "🥜", C}, {"caramell", "🍬", C}, {"chewing", "🍬", C},

                {"acqua", "💧", W}, {"vino", "🍷", W}, {"birra", "🍺", W}, {"birre", "🍺", W}, {"succo", "🥤", W},
                {"succhi", "🥤", W}, {"spremut", "🥤", W}, {"coca", "🥤", W}, {"bibit", "🥤", W}, {"tonica", "🥤", W},
                {"spumante", "🍾", W}, {"prosecco", "🍾", W}, {"liquor", "🥃", W}, {"amaro", "🥃", W}, {"grappa", "🥃", W},

                {"detersiv", "🧴", H}, {"ammorbident", "🧴", H}, {"candeggin", "🧴", H}, {"sgrassator", "🧴", H},
                {"anticalcare", "🧴", H}, {"lavastoviglie", "🧼", H}, {"spugn", "🧽", H}, {"tovagliol", "🧻", H},
                {"fazzolett", "🧻", H}, {"scottex", "🧻", H}, {"sacchi", "🗑️", H}, {"sacchett", "🗑️", H},
                {"pellicola", "📦", H}, {"alluminio", "📦", H}, {"pile ", "🔋", H}, {"batteri", "🔋", H},
                {"lampadin", "💡", H}, {"candel", "🕯️", H}, {"insetticid", "🧴", H},

                {"shampoo", "🧴", X}, {"balsamo", "🧴", X}, {"bagnoschiuma", "🧴", X}, {"docciaschiuma", "🧴", X},
                {"sapone", "🧼", X}, {"dentifric", "🦷", X}, {"spazzolin", "🦷", X}, {"collutorio", "🦷", X},
                {"deodorant", "🧴", X}, {"crema", "🧴", X}, {"rasoi", "🧴", X}, {"schiuma da barba", "🧴", X},
                {"assorbent", "🧴", X}, {"cotton", "🧴", X}, {"pannolin", "👶", X}, {"salviett", "🧻", X},
                {"cerott", "💊", X}, {"medicin", "💊", X}, {"aspirina", "💊", X}, {"tachipirina", "💊", X},

                {"crocchett", "🐾", A}, {"lettiera", "🐱", A}, {"gatto", "🐱", A}, {"gatti", "🐱", A}, {"cane ", "🐶", A},
                {"cani ", "🐶", A},
        };
    }

    private Catalog() {}

    static Match classify(String name) {
        String text = " " + normalize(name) + " ";
        for (Object[] keyword : KEYWORDS)
            if (text.contains(" " + keyword[0]))
                return new Match((Department) keyword[2], (String) keyword[1]);
        return new Match(Department.OTHER, Department.OTHER.emoji);
    }

    // lower case, no accents, only letters and digits: "Caffè d'orzo" → "caffe d orzo"
    private static String normalize(String name) {
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ITALIAN).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
