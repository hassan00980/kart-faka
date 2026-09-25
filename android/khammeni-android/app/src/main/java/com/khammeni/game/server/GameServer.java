package com.khammeni.game.server;

import fi.iki.elonen.NanoHTTPD;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * سيرفر اللعبة على شكل مكتبة Java (NanoHTTPD).
 * بيشتغل في تطبيق الأندرويد نفسه أو على أي كمبيوتر فيه Java.
 */
public class GameServer extends NanoHTTPD {

    /* ------------------- الكاتيجوريات ------------------- */
    public static class Cat {
        final String id, name, emoji;
        final List<String[]> chars = new ArrayList<>();
        Cat(String id, String name, String emoji) { this.id = id; this.name = name; this.emoji = emoji; }
        void add(String[][] list) { for (String[] c : list) chars.add(c); }
    }

    public static final Cat CAT_SPORTS  = new Cat("sports",  "الكورة والرياضة", "⚽");
    public static final Cat CAT_ARTISTS = new Cat("artists", "فنانين ومطربين", "🎤");
    public static final Cat CAT_CELEBS  = new Cat("celebs",  "مشاهير وشخصيات عالمية", "🌟");
    public static final Cat CAT_CARTOON = new Cat("cartoon", "كرتون وأنمي", "🧸");
    public static final Cat CAT_ANIMALS = new Cat("animals", "حيوانات", "🦁");
    public static final Cat CAT_JOBS    = new Cat("jobs",    "وظائف ومهن", "🧑‍⚕️");
    public static final Cat CAT_FOODS   = new Cat("foods",   "أكلات", "🍉");
    public static final Cat CAT_THINGS  = new Cat("things",  "أشياء", "🌳");
    public static final List<Cat> CATS = Arrays.asList(
            CAT_SPORTS, CAT_ARTISTS, CAT_CELEBS, CAT_CARTOON,
            CAT_ANIMALS, CAT_JOBS, CAT_FOODS, CAT_THINGS);

    static {
        // نجوم الكرة (المصريين والعالم) جايين من قاعدة players.json (200 لاعب)
        CAT_SPORTS.add(new String[][]{
                {"نور الشربيني", "🏆"}, {"رامي عاشور", "🏸"}, {"علي فرج", "🎾"}, {"إيهاب أمير", "🥋"},
        });
        CAT_ARTISTS.add(new String[][]{
                {"عمرو دياب", "🎤"}, {"تامر حسني", "🎬"}, {"محمد رمضان", "🦖"}, {"أحمد حلمي", "😄"},
                {"كريم عبد العزيز", "🎭"}, {"محمد منير", "🌙"}, {"شيرين عبد الوهاب", "🎵"}, {"أنغام", "🎼"},
                {"أحمد عز", "🍫"}, {"هند صبري", "🎬"}, {"منى زكي", "⭐"}, {"دنيا سمير غانم", "😂"},
                {"ياسمين صبري", "💃"}, {"هاني شاكر", "🎙️"}, {"محمد حماقي", "🎸"}, {"رامي جمال", "🎹"},
                {"أكرم حسني", "🤣"}, {"يوسف الشريف", "🕶️"}, {"أحمد السقا", "💪"},
                {"نانسي عجرم", "🌸"}, {"إليسا", "🌹"}, {"وائل كفوري", "🎻"}, {"راغب علامة", "🎶"},
        });
        CAT_CELEBS.add(new String[][]{
                {"ليوناردو دي كابريو", "🎥"}, {"توم كروز", "🏍️"}, {"دواين جونسون", "🪨"}, {"إيلون ماسك", "🚗"},
                {"ريهانا", "💎"}, {"بيكاسو", "🎨"}, {"ألبرت أينشتاين", "🧠"}, {"إسحاق نيوتن", "🍎"},
                {"نابليون بونابرت", "🤵"}, {"بيل جيتس", "💻"}, {"ستيف جوبز", "🍏"}, {"محمد علي كلاي", "🥊"},
                {"مايكل جوردان", "🏀"}, {"يوسين بولت", "🏃"}, {"تايلور سويفت", "🎸"}, {"كليوباترا", "👑"},
                {"شارلي شابلن", "🎩"}, {"فان جوخ", "🌻"},
        });
        CAT_CARTOON.add(new String[][]{
                {"سبونج بوب", "🧽"}, {"توم وجيري", "🐱"}, {"ميكي ماوس", "🐭"}, {"بات مان", "🦇"},
                {"سوبر مان", "🦸"}, {"سبايدر مان", "🕷️"}, {"بيكاتشو", "⚡"}, {"شون ذا شيب", "🐑"},
                {"دورا", "🎒"}, {"أولاف", "⛄"}, {"سيمبا", "🦁"}, {"باباي", "🥫"}, {"شريك", "💚"},
                {"علاء الدين", "🧞"}, {"سندريلا", "👗"}, {"هاري بوتر", "🪄"}, {"كابتن ماجد", "⚽"},
                {"باربي", "🩷"}, {"ماشا والدب", "🐻"}, {"سونيك", "🦔"}, {"نيمو", "🐠"},
        });
        CAT_ANIMALS.add(new String[][]{
                {"أسد", "🦁"}, {"فيل", "🐘"}, {"زرافة", "🦒"}, {"بطريق", "🐧"}, {"حصان", "🐴"},
                {"قرد", "🐒"}, {"نمر", "🐯"}, {"دب", "🐻"}, {"تمساح", "🐊"}, {"أرنب", "🐰"},
                {"قطة", "🐱"}, {"كلب", "🐶"}, {"غزال", "🦌"}, {"سلحفاة", "🐢"}, {"بومة", "🦉"},
                {"دولفين", "🐬"}, {"ثعلب", "🦊"}, {"بطة", "🦆"},
        });
        CAT_JOBS.add(new String[][]{
                {"دكتور", "👨‍⚕️"}, {"مهندس", "👷"}, {"طباخ", "👨‍🍳"}, {"رجل إطفاء", "👨‍🚒"},
                {"شرطي", "👮"}, {"مدرس", "👨‍🏫"}, {"رائد فضاء", "👨‍🚀"}, {"مغني", "🎤"}, {"بحار", "⚓"},
                {"سائق", "🚕"}, {"مزارع", "🧑‍🌾"}, {"نجار", "🪚"}, {"محامي", "⚖️"}, {"قاضي", "🧑‍⚖️"},
                {"صياد", "🎣"}, {"مصور", "📷"}, {"طيار", "👨‍✈️"}, {"حلاق", "💈"}, {"جندي", "🪖"},
        });
        CAT_FOODS.add(new String[][]{
                {"بطيخ", "🍉"}, {"مانجو", "🥭"}, {"كشري", "🍛"}, {"شاورما", "🌯"}, {"ذرة مشوي", "🌽"},
                {"فراولة", "🍓"}, {"فول وفلافل", "🫘"}, {"بيتزا", "🍕"}, {"كنافة", "🍮"}, {"بقلاوة", "🍰"},
                {"أم علي", "🍮"}, {"عصير قصب", "🧃"}, {"ملوخية", "🥬"}, {"محشي", "🍅"}, {"كفتة", "🍢"},
                {"تمر", "🌴"}, {"جبنة", "🧀"}, {"عيش بلدي", "🍞"}, {"فسيخ", "🐟"}, {"سلطة", "🥗"},
        });
        CAT_THINGS.add(new String[][]{
                {"شجرة", "🌳"}, {"قمر", "🌙"}, {"شمس", "☀️"}, {"مروحة", "🌀"},
                {"سيارة", "🚗"}, {"موبايل", "📱"}, {"كتاب", "📖"}, {"قلم", "✏️"}, {"ساعة", "⌚"},
                {"نظارة", "👓"}, {"حذاء", "👟"}, {"مفتاح", "🗝️"}, {"طائرة", "✈️"}, {"قطار", "🚂"},
                {"دراجة", "🚲"}, {"تلفزيون", "📺"}, {"لابتوب", "💻"}, {"كوباية شاي", "🍵"},
                {"شمعة", "🕯️"}, {"مظلة", "☂️"},
        });
    }

    /** مصدر ملفات الواجهة */
    public interface Assets {
        InputStream open(String path) throws IOException;
    }

    /* ------------------- الموديلات ------------------- */
    public static class Player {
        String token, name;
        String charName, charEmoji;
        Map<String, Object> charProfile; // البروفايل الكامل لو الشخصية من قاعدة اللاعبين (players.json)
        boolean eliminated;
        boolean isSpy; // في مود الجاسوس بس
        boolean isDoubleAgent; // العميل المزدوج: شبه مدني لكنه مع الجواسيس
        volatile long lastSeen = System.currentTimeMillis();
    }

    /** حالة لعبة «مين الكذاب؟» */
    public static class Liar {
        String phase = "answer"; // answer | reveal | vote | over
        String question, liarQuestion, liarId;
        final Map<String, String> answers = new LinkedHashMap<>(); // token -> نص الإجابة
        final Map<String, String> votes = new LinkedHashMap<>();   // مصوت -> هدف
        boolean caught = false;
        String accusedId;
        final List<String> usedQuestions = new ArrayList<>();
        ChatMsg lastMsg; // رسالة الكشف النهائية
    }

    public static class ChatMsg {
        String from, text;
        long ts;
        boolean system;
        String mediaType, mediaData; // ميديا في الشات (Feature 2)
    }

    public static class Room {
        String code, hostToken;
        String state = "lobby"; // lobby | playing | over
        String mode = "guess"; // guess | spy | auction
        String categoryId = "mix";
        String difficulty = "all"; // all | easy | medium | hard (للكورة في التخمين)
        int spiesCount = 1;
        boolean doubleAgent; // مود الجاسوس: عميل مزدوج شبه مدني شغال للجواسيس
        Boolean spiesWon; // في مود الجاسوس: مين كسب
        String[] word; // الكلمة السرية في مود الجاسوس
        final List<String> usedWords = new ArrayList<>();
        LinkedHashMap<String, Player> players = new LinkedHashMap<>();
        List<ChatMsg> chat = new ArrayList<>();
        List<String> order = new ArrayList<>();
        int turnIndex = 0;
        String winnerId;
        // المزاد
        int squadSize = 5; // 5 | 11
        Auction auction = null;
        List<AuctionResult> auctionResult = null;
        // التصويت (الجاسوس)
        boolean votePrompt = false;
        Map<String, String> voteResponses = new LinkedHashMap<>(); // token -> "now" | "later"
        boolean voteOpen = false;
        List<String> voteTargets = new ArrayList<>();
        Map<String, String> voteMap = new LinkedHashMap<>(); // مصوت -> هدف
        // مين الكذاب؟
        Liar liar = null;
        final List<String> liarUsed = new ArrayList<>();
        // النقاط
        Map<String, Integer> points = new LinkedHashMap<>();
    }

    /** كارت فيفا للمزاد (من players.json) */
    public static class FifaCard {
        String n, pos, t, c, e;
        int r, v, num, price;
    }

    /** حالة المزاد الجارية في الأوضة */
    public static class Auction {
        final List<FifaCard> deck = new ArrayList<>();
        int cardIndex = 0;
        int currentBid = 0;
        int bidTurn = 0;
        final List<String> bidders = new ArrayList<>();
        final Map<String, Integer> budgets = new LinkedHashMap<>();
        final Map<String, List<FifaCard>> teams = new LinkedHashMap<>();
        int needed = 0;
        final List<FifaCard> poolLeft = new ArrayList<>();
        final List<FifaCard> spare = new ArrayList<>();
        String phase = "bidding"; // bidding | steal
        final List<String> stealOrder = new ArrayList<>();
        int stealTurn = 0;
        final Map<String, Boolean> stealUsed = new LinkedHashMap<>();
    }

    /** نتيجة مزاد منتهي (تشكيلة كل لاعب) */
    public static class AuctionResult {
        String id;
        int total;
        final List<FifaCard> team = new ArrayList<>();
    }

    /* ------------------- ثوابت المزاد ------------------- */
    static final String[] FORM_5 = {"GK", "DF", "DF", "MF", "FW"};
    static final String[] FORM_11 = {"GK", "DF", "DF", "DF", "DF", "MF", "MF", "MF", "FW", "FW", "FW"};
    static final Map<String, String> POS_EMOJI = new LinkedHashMap<>();
    static {
        POS_EMOJI.put("GK", "🧤");
        POS_EMOJI.put("DF", "🛡️");
        POS_EMOJI.put("MF", "🎯");
        POS_EMOJI.put("FW", "⚽");
    }
    static String[] formFor(int size) { return size == 11 ? FORM_11 : FORM_5; }
    static int priceOf(int r) {
        if (r >= 89) return 90; if (r >= 86) return 65; if (r >= 83) return 45;
        if (r >= 80) return 30; if (r >= 77) return 20; if (r >= 74) return 13;
        if (r >= 71) return 9; return 6;
    }
    static int cardBase(FifaCard c) { return Math.max(5, Math.round(c.v * 0.6f)); }

    /* ------------------- الحالة ------------------- */
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> tokenRoom = new ConcurrentHashMap<>(); // token -> code
    /** قاعدة بيانات اللاعبين (players.json) */
    private final List<String[]> playerPool = new ArrayList<>();                       // {اسم, علم} للاختيار
    private final Map<String, Map<String, Object>> playerProfiles = new HashMap<>();  // nl(name) -> البروفايل
    private final Map<String, String> clubColors = new HashMap<>();                   // اسم النادي/الفريق -> لون القميص
    /** صور الشخصيات (char-img.json) ووصفها (char-bio.json) */
    private final Map<String, String> charImg = new HashMap<>();                      // الاسم العربي -> "slug.ext"
    private final Map<String, String> charBio = new HashMap<>();                      // الاسم العربي -> وصف مختصر
    /** أسئلة لعبة «مين الكذاب؟» */
    private final List<String> liarQuestions = new ArrayList<>();
    private final Assets assets;
    private final ScheduledExecutorService clock =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "game-clock");
                t.setDaemon(true);
                return t;
            });

    public static volatile int currentPort = 3000;

    public GameServer(int port, Assets assets) {
        super(port);
        this.assets = assets;
        currentPort = port;
        loadPlayerDb();
        loadStringMap("char-img.json", charImg);
        loadStringMap("char-bio.json", charBio);
        loadLiarQuestions();
    }

    /* ---------------------------------------------
       Protocol Version — بيمنع دخول عملاء قديمة على سيرفر جديد
       لو نسخة العميل (v) أقل من اللي هنا أو غايبة → رفض + رسالة تحديث
       --------------------------------------------- */
    public static final int PROTOCOL_VERSION = 6;
    private static final String VERSION_MESSAGE = "⚠️ نسختك قديمة — حدّث التطبيق من السيرفر أو أعد فتح الصفحة";

    /** تحميل أسئلة «مين الكذاب؟» من الأصول (نفس ملف الويب بالظبط) */
    private void loadLiarQuestions() {
        try {
            InputStream in = assets.open("liar-questions.json");
            String txt = new String(readAll(in), StandardCharsets.UTF_8);
            Object root = MiniJson.parse(txt);
            if (root instanceof List) {
                for (Object o : (List<?>) root) {
                    Map<String, Object> q = MiniJson.obj(o);
                    Object v = q.get("q");
                    if (v instanceof String && !((String) v).trim().isEmpty()) {
                        liarQuestions.add((String) v);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[كرت فكة] فشل تحميل liar-questions.json: " + e);
        }
    }

    /* تحميل خريطة نصية من assets (char-img.json / char-bio.json) */
    private void loadStringMap(String file, Map<String, String> out) {
        try {
            InputStream in = assets.open(file);
            String txt = new String(readAll(in), StandardCharsets.UTF_8);
            Map<String, Object> root = MiniJson.obj(MiniJson.parse(txt));
            for (Map.Entry<String, Object> e : root.entrySet()) {
                Object v = e.getValue();
                if (v != null) out.put(e.getKey(), String.valueOf(v));
            }
        } catch (Exception e) {
            System.err.println("[كرت فكة] فشل تحميل " + file + ": " + e);
        }
    }

    public void start() throws IOException {
        super.start(SOCKET_READ_TIMEOUT, true);
        clock.scheduleAtFixedRate(this::tick, 10, 5, TimeUnit.SECONDS);
    }

    public void stop() {
        clock.shutdownNow();
        super.stop();
    }

    /* ------------------- تطبيع الاسم ------------------- */
    public static String nl(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase();
        s = s.replaceAll("[\\u064B-\\u0652\\u0640]", "");
        s = s.replaceAll("[\\u0623\\u0625\\u0622]", "\u0627"); // أ إ آ -> ا
        s = s.replace('\u0649', '\u064A'); // ى -> ي
        s = s.replace('\u0629', '\u0647'); // ة -> ه
        s = s.replace('\u0626', '\u064A'); // ئ -> ي
        s = s.replace('\u0624', '\u0648'); // ؤ -> و
        s = s.replace("\u0621", ""); // ء
        s = s.replaceAll("\\s+", " ");
        return s.trim();
    }

    /* ------------------- أدوات ------------------- */
    private static String genCode(Map<String, Room> rooms) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String code;
        do {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 5; i++) b.append(chars.charAt((int) (Math.random() * chars.length())));
            code = b.toString();
        } while (rooms.containsKey(code));
        return code;
    }

    public static List<String> getIps() {
        List<String> out = new ArrayList<>();
        try {
            for (java.util.Enumeration<NetworkInterface> ens = NetworkInterface.getNetworkInterfaces();
                 ens.hasMoreElements(); ) {
                NetworkInterface n = ens.nextElement();
                if (n.isLoopback() || !n.isUp()) continue;
                for (java.util.Enumeration<InetAddress> as = n.getInetAddresses(); as.hasMoreElements(); ) {
                    InetAddress a = as.nextElement();
                    if (a instanceof Inet4Address) out.add(a.getHostAddress());
                }
            }
        } catch (Exception ignored) { }
        return out;
    }

    /* ------------------- قاعدة بيانات اللاعبين (players.json) ------------------- */
    private void loadPlayerDb() {
        try {
            InputStream in = assets.open("players.json");
            String txt = new String(readAll(in), StandardCharsets.UTF_8);
            Map<String, Object> root = MiniJson.obj(MiniJson.parse(txt));
            Map<String, Object> colors = MiniJson.obj(root.get("colors"));
            for (Map.Entry<String, Object> e : colors.entrySet()) {
                clubColors.put(e.getKey(), String.valueOf(e.getValue()));
            }
            Object ps = root.get("players");
            if (ps instanceof List) {
                for (Object o : (List<?>) ps) {
                    Map<String, Object> pm = MiniJson.obj(o);
                    Object n = pm.get("n");
                    if (!(n instanceof String)) continue;
                    String name = (String) n;
                    if (name.trim().isEmpty()) continue;
                    playerProfiles.put(nl(name), pm);
                    String country = pm.get("c") == null ? "" : String.valueOf(pm.get("c"));
                    playerPool.add(new String[]{name, flagEmoji(country)});
                }
            }
        } catch (Exception e) {
            System.err.println("[كرت فكة] فشل تحميل players.json: " + e);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        return out.toByteArray();
    }

    /** لون قميص النادي الحالي من قاعدة الألوان */
    private String jerseyColor(Map<String, Object> prof) {
        Object t = prof.get("t");
        String c = t == null ? null : clubColors.get(String.valueOf(t));
        return c == null ? "#24243a" : c;
    }

    /* صورة الشخصية المخزنة في التطبيق (بحث بالاسم أو بابن اللاعب أو بالأحرف الإنجليزية) */
    private String charImageOf(String name, Map<String, Object> profile) {
        if (name != null && charImg.containsKey(name)) return charImg.get(name);
        if (profile != null) {
            Object n = profile.get("n");
            if (n != null && charImg.containsKey(String.valueOf(n))) return charImg.get(String.valueOf(n));
            Object e = profile.get("e");
            if (e != null) {
                String slug = slugOf(String.valueOf(e));
                for (String v : charImg.values()) {
                    if (v.startsWith(slug + ".")) return v;
                }
            }
        }
        return null;
    }

    private static String slugOf(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    private static String flagEmoji(String country) {
        switch (country == null ? "" : country) {
            case "مصر": return "🇪🇬";
            case "الجزائر": return "🇩🇿";
            case "المغرب": return "🇲🇦";
            case "السعودية": return "🇸🇦";
            case "قطر": return "🇶🇦";
            case "تونس": return "🇹🇳";
            case "البرازيل": return "🇧🇷";
            case "الأرجنتين": return "🇦🇷";
            case "إيطاليا": return "🇮🇹";
            case "هولندا": return "🇳🇱";
            case "ألمانيا": return "🇩🇪";
            case "إنجلترا": return "🏴󠁧󠁢󠁥󠁮󠁧󠁿";
            case "البرتغال": return "🇵🇹";
            case "فرنسا": return "🇫🇷";
            case "ساحل العاج": return "🇨🇮";
            case "الكاميرون": return "🇨🇲";
            case "ليبيريا": return "🇱🇷";
            case "نيجيريا": return "🇳🇬";
            case "المكسيك": return "🇲🇽";
            case "كولومبيا": return "🇨🇴";
            case "كوريا الجنوبية": return "🇰🇷";
            case "الدنمارك": return "🇩🇰";
            case "السويد": return "🇸🇪";
            case "أوروغواي": return "🇺🇾";
            case "ويلز": return "🏴󠁧󠁢󠁷󠁬󠁳󠁿";
            case "بلجيكا": return "🇧🇪";
            case "النرويج": return "🇳🇴";
            case "إسبانيا": return "🇪🇸";
            case "بولندا": return "🇵🇱";
            case "كرواتيا": return "🇭🇷";
            case "تركيا": return "🇹🇷";
            case "السنغال": return "🇸🇳";
            case "الغابون": return "🇬🇦";
            case "الإكوادور": return "🇪🇨";
            case "النمسا": return "🇦🇹";
            case "صربيا": return "🇷🇸";
            case "الولايات المتحدة": return "🇺🇸";
            case "المجر": return "🇭🇺";
            default: return "⚽";
        }
    }

    /** عدد لاعبي القاعدة المحمّلين (للاختبار) */
    public int dbPoolSize() { return playerPool.size(); }

    /** بروفايل لاعب بالاسم (للاختبار) */
    public Map<String, Object> testProfileOf(String name) { return playerProfiles.get(nl(name)); }

    /** قاعدة الكورة: النشطين الثابتين + لاعبي players.json (من غير تكرار) */
    private List<String[]> sportsPool() {
        List<String[]> pool = new ArrayList<>(CAT_SPORTS.chars);
        Set<String> seen = new HashSet<>();
        for (String[] c : CAT_SPORTS.chars) seen.add(c[0]);
        for (String[] c : playerPool) if (seen.add(c[0])) pool.add(c);
        return pool;
    }

    /** كل الشخصيات (للكاتيجوري العشوائي) */
    private List<String[]> allPool() {
        List<String[]> pool = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Cat cat : CATS) {
            for (String[] c : cat.chars) if (seen.add(c[0])) pool.add(c);
        }
        for (String[] c : playerPool) if (seen.add(c[0])) pool.add(c);
        return pool;
    }

    private List<String[]> charsOf(String categoryId) {
        if (categoryId == null || "mix".equals(categoryId)) return allPool();
        if ("sports".equals(categoryId)) return sportsPool();
        for (Cat cat : CATS) if (cat.id.equals(categoryId)) return cat.chars;
        return allPool();
    }

    /** مستويات صعوبة الكورة: سهل = أعلى تقييم (الأشهر) */
    private static String tierOfRating(int r) {
        if (r >= 89) return "easy";
        if (r >= 84) return "medium";
        return "hard";
    }

    private static boolean isDiff(String d) {
        return "all".equals(d) || "easy".equals(d) || "medium".equals(d) || "hard".equals(d);
    }

    private static String diffName(String d) {
        if ("easy".equals(d)) return "سهل 🟢";
        if ("medium".equals(d)) return "متوسط 🟡";
        if ("hard".equals(d)) return "صعب 🔴";
        return "الكل";
    }

    /** قاعدة الكورة مصفّاة بمستوى صعوبة (للتخمين بس) */
    private List<String[]> sportsPoolFiltered(String difficulty) {
        List<String[]> pool = sportsPool();
        if (!isDiff(difficulty) || "all".equals(difficulty)) return pool;
        List<String[]> out = new ArrayList<>();
        for (String[] c : pool) {
            Map<String, Object> prof = playerProfiles.get(nl(c[0]));
            int r = -1;
            if (prof != null && prof.get("r") instanceof Number) r = ((Number) prof.get("r")).intValue();
            String tier = r < 0 ? "easy" : tierOfRating(r); // الثابتين/الأساطير من غير تقييم => سهل
            if (difficulty.equals(tier)) out.add(c);
        }
        return out;
    }

    /** لستة الكاتيجوريات اللي بنبعتها للواجهة */
    private List<Object> categoryInfo() {
        List<Object> out = new ArrayList<>();
        Map<String, Object> mix = new LinkedHashMap<>();
        mix.put("id", "mix");
        mix.put("name", "عشوائي (مخلوط)");
        mix.put("emoji", "🎲");
        out.add(mix);
        for (Cat cat : CATS) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", cat.id);
            m.put("name", cat.name);
            m.put("emoji", cat.emoji);
            out.add(m);
        }
        return out;
    }

    private List<String[]> pickCharacters(int n, String categoryId, Set<String> exclude, String difficulty) {
        List<String[]> base = "sports".equals(categoryId) && isDiff(difficulty) ? sportsPoolFiltered(difficulty) : charsOf(categoryId);
        List<String[]> pool = new ArrayList<>();
        for (String[] c : base) {
            if (exclude == null || !exclude.contains(c[0])) pool.add(c);
        }
        Collections.shuffle(pool);
        if (pool.size() <= n) return pool;
        return pool.subList(0, n);
    }

    private List<Player> actives(Room r) {
        List<Player> list = new ArrayList<>();
        for (Player p : r.players.values()) if (!p.eliminated) list.add(p);
        return list;
    }

    private Room roomOf(String token) {
        if (token == null) return null;
        String code = tokenRoom.get(token);
        return code == null ? null : rooms.get(code);
    }

    /* ------------------- منطق الدور ------------------- */
    private void nextTurn(Room r) {
        List<Player> acts = actives(r);
        if (acts.size() <= 1) return;
        List<String> activeIds = new ArrayList<>();
        for (Player p : acts) activeIds.add(p.token);
        if (r.order.isEmpty()) return;
        int i = (r.turnIndex + 1) % r.order.size();
        for (int s = 0; s < r.order.size(); s++) {
            String cid = r.order.get(i);
            if (activeIds.contains(cid) && r.players.get(cid) != null) {
                r.turnIndex = i;
                return;
            }
            i = (i + 1) % r.order.size();
        }
    }

    private boolean checkGameOver(Room r) {
        List<Player> acts = actives(r);
        if (acts.size() == 1) {
            r.state = "over";
            r.winnerId = acts.get(0).token;
            return true;
        }
        return false;
    }

    private boolean checkSpyGameOver(Room r) {
        List<Player> acts = actives(r);
        List<Player> spies = new ArrayList<>(), da = new ArrayList<>(), civs = new ArrayList<>();
        for (Player p : acts) {
            if (p.isSpy) spies.add(p);
            else if (p.isDoubleAgent) da.add(p);
            else civs.add(p);
        }
        if (spies.isEmpty() && da.isEmpty()) {
            r.state = "over";
            r.spiesWon = false;
            for (Player c : civs) addPoints(r, c.token, 1);
            return true;
        }
        if (civs.size() <= spies.size() + da.size()) {
            r.state = "over";
            r.spiesWon = true;
            for (Player s : spies) addPoints(r, s.token, 3);
            for (Player d : da) addPoints(r, d.token, 3);
            return true;
        }
        return false;
    }

    /* ------------------- أفعال اللاعبين ------------------- */
    private void startGuessRound(Room r, List<Player> acts, List<String[]> pool) {
        for (int i = 0; i < acts.size(); i++) {
            Player p = acts.get(i);
            p.isSpy = false;
            p.isDoubleAgent = false;
            if (i < pool.size()) {
                p.charName = pool.get(i)[0];
                p.charEmoji = pool.get(i)[1];
            } else {
                p.charName = null;
                p.charEmoji = null;
            }
            p.charProfile = p.charName == null ? null : playerProfiles.get(nl(p.charName));
            p.eliminated = false;
        }
        eliminateNonActives(r, acts);
        r.order = new ArrayList<>();
        for (Player p : acts) r.order.add(p.token);
        Collections.shuffle(r.order);
        r.turnIndex = 0;
        r.winnerId = null;
        r.spiesWon = null;
        r.votePrompt = false;
        r.voteResponses.clear();
        r.voteOpen = false;
        r.voteTargets.clear();
        r.voteMap.clear();
        r.state = "playing";
    }

    private void startSpyRound(Room r, List<Player> acts) {
        String catId = "mix".equals(r.categoryId) ? "sports" : r.categoryId;
        List<String[]> pool = charsOf(catId);
        List<String[]> fresh = new ArrayList<>();
        for (String[] c : pool) if (!r.usedWords.contains(c[0])) fresh.add(c);
        List<String[]> base = fresh.isEmpty() ? pool : fresh;
        String[] word = base.get((int) (Math.random() * base.size()));
        r.usedWords.add(word[0]);
        r.word = word;

        int spies = Math.min(r.spiesCount, Math.max(1, acts.size() - 1));
        List<Player> shuffled = new ArrayList<>(acts);
        Collections.shuffle(shuffled);
        Set<String> spyIds = new HashSet<>();
        for (int i = 0; i < spies; i++) spyIds.add(shuffled.get(i).token);
        String doubleAgentId = null;
        boolean daEnabled = r.doubleAgent && acts.size() >= 3;
        if (daEnabled) {
            for (Player pl : shuffled) {
                if (!spyIds.contains(pl.token)) { doubleAgentId = pl.token; break; }
            }
        }

        for (Player p : acts) {
            p.isSpy = spyIds.contains(p.token);
            p.isDoubleAgent = p.token.equals(doubleAgentId);
            p.eliminated = false;
            p.charName = p.isSpy ? "جاسوس" : word[0];
            p.charEmoji = p.isSpy ? "🕵️" : word[1];
            p.charProfile = null;
        }
        eliminateNonActives(r, acts);
        r.order = new ArrayList<>();
        for (Player p : acts) r.order.add(p.token);
        Collections.shuffle(r.order);
        r.turnIndex = 0;
        r.winnerId = null;
        r.spiesWon = null;
        r.votePrompt = false;
        r.voteResponses.clear();
        r.voteOpen = false;
        r.voteTargets.clear();
        r.voteMap.clear();
        r.state = "playing";
        ChatMsg m = new ChatMsg();
        m.from = "النظام";
        m.system = true;
        Cat cat = null;
        for (Cat c : CATS) if (c.id.equals(catId)) { cat = c; break; }
        m.text = "🕵️ لعبة الجاسوس بدأت! الكل شايف الكلمة... إلا الجواسيس";
        if (cat != null) m.text += " (الكاتيجوري: " + cat.emoji + " " + cat.name + ")";
        if (daEnabled) m.text += " · وفي عميل مزدوج بينكم 🎭 (شكله مدني لكنه شغال للجواسيس)!";
        m.ts = System.currentTimeMillis();
        r.chat.add(m);
    }

    private void eliminateNonActives(Room r, List<Player> acts) {
        Set<String> actTokens = new HashSet<>();
        for (Player p : acts) actTokens.add(p.token);
        for (Player p : r.players.values()) {
            if (!actTokens.contains(p.token)) {
                p.eliminated = true;
                p.charName = null;
                p.charEmoji = null;
                p.charProfile = null;
                p.isSpy = false;
                p.isDoubleAgent = false;
            }
        }
    }

    /** يبدأ الجولة (تخمين أو جاسوس). يرجع false لو مفيش شروط اكتملت. */
    private boolean attemptStart(Room r) {
        // كل اللاعبين المتصلين (= الكل في نموذج الاستطلاع) — الاستبعاد بيبقى للجولة الحالية بس
        List<Player> acts = new ArrayList<>(r.players.values());
        if (acts.size() < 2) return false;
        if ("auction".equals(r.mode)) {
            if (acts.size() < 2) return false;
            startAuction(r, acts);
            return true;
        }
        if ("spy".equals(r.mode)) {
            if (acts.size() < 3) return false;
            startSpyRound(r, acts);
            return true;
        }
        if ("liar".equals(r.mode)) {
            if (acts.size() < 3) return false;
            return startLiarRound(r) == null;
        }
        Set<String> excl = new HashSet<>();
        for (Player p : r.players.values()) if (p.charName != null) excl.add(p.charName);
        List<String[]> pool = pickCharacters(acts.size(), r.categoryId, excl, r.difficulty);
        if (pool.size() < acts.size()) return false;
        startGuessRound(r, acts, pool);
        ChatMsg gm = new ChatMsg();
        gm.from = "النظام";
        gm.system = true;
        gm.ts = System.currentTimeMillis();
        boolean diffSports = "sports".equals(r.categoryId) && isDiff(r.difficulty) && !"all".equals(r.difficulty);
        gm.text = diffSports
            ? "🎭 التخمين بدأ! الكورة بسهولة «" + diffName(r.difficulty) + "» — كل واحد شايف شخصيته ويسألوا بعض بنعم/لا"
            : "🎭 التخمين بدأ! كل واحد شايف شخصيته السرية ويسألوا بعض بنعم/لا";
        r.chat.add(gm);
        return true;
    }

    private void startGame(String token) {
        Room r = roomOf(token);
        if (r == null || !r.hostToken.equals(token)) return;
        attemptStart(r);
    }

    private void nextRound(String token) {
        Room r = roomOf(token);
        if (r == null || !r.hostToken.equals(token)) return;
        if (r.players.size() < 2) return;
        if ("auction".equals(r.mode)) {
            r.auction = null;
            r.auctionResult = null;
        }
        attemptStart(r);
    }

    private void guess(String token, String targetId, String guess) {
        Room r = roomOf(token);
        if (r == null || !"playing".equals(r.state)) return;
        if ("liar".equals(r.mode)) return; // مود مين الكذاب من غير تخمين أدوار
        Player me = r.players.get(token);
        if (me == null || me.eliminated) return;
        Player target = r.players.get(targetId);
        if (target == null || target == me || target.eliminated || target.charName == null) return;

        ChatMsg m = new ChatMsg();
        m.from = "النظام";
        m.system = true;

        // مود الجاسوس: اتهام بدون كلمة مكتوبة
        if ("spy".equals(r.mode)) {
            if (target.isSpy) {
                r.state = "over";
                r.spiesWon = false;
                addPoints(r, me.token, 3);
                for (Player x : actives(r)) {
                    if (!x.token.equals(me.token) && !x.isSpy && !x.isDoubleAgent) addPoints(r, x.token, 1);
                }
                m.text = "🎉 " + me.name + " قبض على الجاسوس! " + target.name + " كان جاسوس 🕵️"
                        + (r.word != null ? " (الكلمة كانت: " + r.word[1] + " " + r.word[0] + ")" : "");
            } else {
                me.eliminated = true;
                m.text = "😅 " + me.name + " اتهم " + target.name + " بالغلط — "
                        + (target.isDoubleAgent ? "عميل مزدوج! 🎭" : "مدني طبيعي! ")
                        + me.name + " اتقبض عليه بدل الجاسوس";
                if (!checkSpyGameOver(r)) nextTurn(r);
            }
            r.chat.add(m);
            return;
        }

        // مود التخمين
        if (guess == null || guess.trim().isEmpty()) return;
        boolean correct = nl(guess).equals(nl(target.charName));
        if (correct) {
            target.eliminated = true;
            m.text = "🎉 صح! " + me.name + " عرف إن " + target.name + " هو " + target.charEmoji + " " + target.charName;
        } else {
            me.eliminated = true;
            m.text = "❌ غلط! " + me.name + " خمّن غلط واتخرج من الجولة (الشخصية الصح كانت "
                    + target.charEmoji + " " + target.charName + ")";
        }
        r.chat.add(m);

        if (!checkGameOver(r)) nextTurn(r);
    }

    private void passTurn(String token) {
        Room r = roomOf(token);
        if (r == null || !"playing".equals(r.state)) return;
        if ("liar".equals(r.mode) || "auction".equals(r.mode)) return;
        if (r.order.isEmpty() || r.turnIndex < 0 || r.turnIndex >= r.order.size()) return;
        if (!r.order.get(r.turnIndex).equals(token)) return;
        nextTurn(r);
    }

    private void leave(Room r, Player p) {
        if (p == null) return;
        r.players.remove(p.token);
        tokenRoom.remove(p.token);
        if (r.players.size() == 0) {
            rooms.remove(r.code);
            return;
        }
        if (r.hostToken.equals(p.token)) {
            r.hostToken = r.players.keySet().iterator().next();
        }
        ChatMsg m = new ChatMsg();
        m.from = "النظام";
        m.system = true;
        m.text = "👋 " + p.name + " ساب الأوضة";
        r.chat.add(m);
        if ("playing".equals(r.state)) {
            if ("auction".equals(r.mode)) {
                handleAuctionLeave(r, p.token);
                return;
            }
            if ("liar".equals(r.mode)) {
                Liar L = r.liar;
                if (L != null) {
                    if (L.liarId != null && L.liarId.equals(p.token) && !"over".equals(L.phase)) {
                        L.phase = "over";
                        L.caught = false;
                        r.state = "over";
                        for (Player a : actives(r)) addPoints(r, a.token, 3);
                        ChatMsg m2 = new ChatMsg();
                        m2.from = "النظام";
                        m2.system = true;
                        m2.ts = System.currentTimeMillis();
                        m2.text = "😱 " + p.name + " كان الكذاب وساب الأوضة! الباقيين كسبوا";
                        r.chat.add(m2);
                    } else if ("answer".equals(L.phase)) {
                        boolean all = true;
                        for (Player a : actives(r)) {
                            if (!L.answers.containsKey(a.token)) { all = false; break; }
                        }
                        if (actives(r).size() >= 2 && all) {
                            L.phase = "reveal";
                            ChatMsg m3 = new ChatMsg();
                            m3.from = "النظام";
                            m3.system = true;
                            m3.ts = System.currentTimeMillis();
                            m3.text = "📜 اتعرضت الإجابات! كل واحد يشوف إجابات الكل وشوفوا مين جوابه مش ماشي مع السؤال 🧐";
                            r.chat.add(m3);
                        }
                    }
                }
                return;
            }
            if ("spy".equals(r.mode)) {
                if (p.isSpy) {
                    r.state = "over";
                    r.spiesWon = false;
                    ChatMsg m2 = new ChatMsg();
                    m2.from = "النظام";
                    m2.system = true;
                    m2.text = "😱 " + p.name + " كان الجاسوس وساب الأوضة! المدنيين كسبوا";
                    m2.ts = System.currentTimeMillis();
                    r.chat.add(m2);
                } else if (p.isDoubleAgent) {
                    ChatMsg m2 = new ChatMsg();
                    m2.from = "النظام";
                    m2.system = true;
                    m2.text = "🎭 " + p.name + " كان العميل المزدوج وساب الأوضة!";
                    m2.ts = System.currentTimeMillis();
                    r.chat.add(m2);
                    if (!checkSpyGameOver(r)) {
                        String cur = r.order.isEmpty() || r.turnIndex >= r.order.size() ? null : r.order.get(r.turnIndex);
                        if (cur == null || r.players.get(cur) == null) nextTurn(r);
                    }
                } else if (!checkSpyGameOver(r)) {
                    String cur = r.order.isEmpty() || r.turnIndex >= r.order.size() ? null : r.order.get(r.turnIndex);
                    if (cur == null || r.players.get(cur) == null) nextTurn(r);
                }
            } else {
                if (!checkGameOver(r)) {
                    String cur = r.order.isEmpty() || r.turnIndex >= r.order.size() ? null : r.order.get(r.turnIndex);
                    if (cur == null || r.players.get(cur) == null) nextTurn(r);
                }
            }
        }
    }

    /* ------------------- منطق لعبة «مين الكذاب؟» 🤥 ------------------- */
    private Liar newLiarRound(Room r) {
        List<String> fresh = new ArrayList<>();
        for (String q : liarQuestions) {
            if (!r.liarUsed.contains(q)) fresh.add(q);
        }
        List<String> pool = fresh.size() >= 2 ? fresh : liarQuestions;
        String q1 = pool.get((int) (Math.random() * pool.size()));
        String q2 = q1;
        while (q2.equals(q1)) q2 = pool.get((int) (Math.random() * pool.size()));
        List<Player> acts = new ArrayList<>(r.players.values());
        String liarId = acts.get((int) (Math.random() * acts.size())).token;
        for (Player p : r.players.values()) {
            p.eliminated = false;
            p.isSpy = false;
            p.isDoubleAgent = false;
            p.charName = null;
            p.charEmoji = null;
            p.charProfile = null;
        }
        Liar L = new Liar();
        L.phase = "answer";
        L.question = q1;
        L.liarQuestion = q2;
        L.liarId = liarId;
        L.usedQuestions.add(q1);
        r.liarUsed.add(q1);
        if (r.liarUsed.size() > 40) r.liarUsed.remove(0);
        r.liar = L;
        r.state = "playing";
        return L;
    }

    /** بدء جولة مين الكذاب — يرجع رسالة خطأ أو null لو نجح */
    private String startLiarRound(Room r) {
        List<Player> acts = new ArrayList<>(r.players.values());
        if (acts.size() < 3) return "لعبة مين الكذاب محتاجة 3 لاعبين على الأقل 🤥";
        newLiarRound(r);
        ChatMsg m = new ChatMsg();
        m.from = "النظام";
        m.system = true;
        m.ts = System.currentTimeMillis();
        m.text = "🤥 مين الكذاب بدأت! الكل شايف السؤال... وواحد فيكم شايف سؤال مختلف — جوابه هيبوح بيه 😏 كل واحد يكتب إجابته في السر";
        r.chat.add(m);
        return null;
    }

    private void liarAnswer(Room r, Player p, String text) {
        if (r.liar == null || !"playing".equals(r.state)) return;
        if (!"answer".equals(r.liar.phase)) return;
        if (p.eliminated) return;
        if (text == null || text.trim().isEmpty()) return;
        r.liar.answers.put(p.token, text.trim().substring(0, Math.min(120, text.trim().length())));
        boolean allAnswered = true;
        for (Player a : actives(r)) {
            if (!r.liar.answers.containsKey(a.token)) { allAnswered = false; break; }
        }
        if (allAnswered) {
            r.liar.phase = "reveal";
            ChatMsg m = new ChatMsg();
            m.from = "النظام";
            m.system = true;
            m.ts = System.currentTimeMillis();
            m.text = "📜 اتعرضت الإجابات! كل واحد يشوف إجابات الكل وشوفوا مين جوابه مش ماشي مع السؤال 🧐";
            r.chat.add(m);
        }
    }

    private void liarStartVote(Room r, String token) {
        if (r.liar == null || !"reveal".equals(r.liar.phase)) return;
        r.liar.phase = "vote";
        r.liar.votes.clear();
        ChatMsg m = new ChatMsg();
        m.from = "النظام";
        m.system = true;
        m.ts = System.currentTimeMillis();
        m.text = "🗳️ التصويت بدأ! كل واحد يصوت على اللي شاكك إنه الكذاب";
        r.chat.add(m);
    }

    private void liarCastVote(Room r, Player p, String targetId) {
        if (r.liar == null || !"vote".equals(r.liar.phase)) return;
        if (p.eliminated) return;
        Player t = r.players.get(targetId);
        if (t == null || t == p || t.eliminated) return;
        r.liar.votes.put(p.token, targetId);
        boolean allVoted = true;
        for (Player a : actives(r)) {
            if (!r.liar.votes.containsKey(a.token)) { allVoted = false; break; }
        }
        if (allVoted) resolveLiar(r);
    }

    private void resolveLiar(Room r) {
        Liar L = r.liar;
        if (L == null || !"vote".equals(L.phase)) return;
        L.phase = "over";
        List<Player> acts = actives(r);
        Map<String, Integer> tally = new LinkedHashMap<>();
        for (Player a : acts) {
            String t = L.votes.get(a.token);
            if (t != null) tally.put(t, (tally.containsKey(t) ? tally.get(t) : 0) + 1);
        }
        String top = null;
        int topVotes = 0, secondVotes = 0;
        for (Map.Entry<String, Integer> e : tally.entrySet()) {
            int v = e.getValue();
            if (v > topVotes) { secondVotes = topVotes; topVotes = v; top = e.getKey(); }
            else if (v > secondVotes) secondVotes = v;
        }
        boolean tie = top == null || (tally.size() > 1 && topVotes == secondVotes);
        String accusedId = tie ? null : top;
        Player liar = r.players.get(L.liarId);
        boolean caught = accusedId != null && accusedId.equals(L.liarId);
        L.caught = caught;
        L.accusedId = accusedId;
        r.state = "over";
        Player victim = accusedId == null ? null : r.players.get(accusedId);
        ChatMsg m = new ChatMsg();
        m.from = "النظام";
        m.system = true;
        m.ts = System.currentTimeMillis();
        if (caught) {
            for (Player a : acts) {
                if (L.votes.get(a.token) != null && L.votes.get(a.token).equals(L.liarId)) addPoints(r, a.token, 3);
                else if (!a.token.equals(L.liarId)) addPoints(r, a.token, 1);
            }
            m.text = "🎉 قبضنا على الكذاب! " + (liar == null ? "؟" : liar.name)
                    + " كان شايف سؤال تاني: «" + L.liarQuestion + "» وجاوب «" + L.answers.get(L.liarId) + "» — إجابته فضحته!";
        } else {
            addPoints(r, L.liarId, 3);
            m.text = "😏 الكذاب نجا! " + (liar == null ? "؟" : liar.name)
                    + " كان شايف سؤال تاني: «" + L.liarQuestion + "» وجاوب «" + L.answers.get(L.liarId) + "»"
                    + (victim == null ? " — وما حدش اتقبض عليه (تعادل!)" : " — والناس اتهمت " + victim.name + " بالغلط!");
        }
        L.lastMsg = m;
        r.chat.add(m);
    }

    /** عرض لعبة مين الكذاب لمسار محدد (إساءة اللاعبين في sanitize) */
    private Map<String, Object> liarView(Room r, String token) {
        Liar L = r.liar;
        Map<String, Object> m = new LinkedHashMap<>();
        if (L == null) return null;
        boolean isLiar = L.liarId != null && L.liarId.equals(token);
        m.put("phase", L.phase);
        m.put("question", isLiar ? L.liarQuestion : L.question);
        m.put("myAnswer", L.answers.get(token));
        m.put("iAmLiar", isLiar);
        m.put("answeredCount", L.answers.size());
        m.put("total", actives(r).size());
        if ("answer".equals(L.phase)) {
            m.put("answers", null);
        } else {
            List<Object> ans = new ArrayList<>();
            for (Player p : actives(r)) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("id", p.token);
                a.put("name", p.name);
                a.put("text", L.answers.get(p.token) == null ? "—" : L.answers.get(p.token));
                a.put("isMine", p.token.equals(token));
                ans.add(a);
            }
            m.put("answers", ans);
        }
        if ("vote".equals(L.phase)) {
            List<Object> targets = new ArrayList<>();
            for (Player p : actives(r)) {
                if (!p.token.equals(token)) {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("id", p.token);
                    t.put("name", p.name);
                    targets.add(t);
                }
            }
            m.put("targets", targets);
            m.put("myVote", L.votes.get(token));
            m.put("votesCast", L.votes.size());
        } else {
            m.put("targets", null);
            m.put("myVote", null);
            m.put("votesCast", 0);
        }
        if ("over".equals(L.phase)) {
            Map<String, Object> rev = new LinkedHashMap<>();
            rev.put("question", L.question);
            rev.put("liarQuestion", L.liarQuestion);
            rev.put("liarId", L.liarId);
            Player lr = r.players.get(L.liarId);
            rev.put("liarName", lr == null ? "؟" : lr.name);
            rev.put("liarAnswer", L.answers.get(L.liarId));
            rev.put("caught", L.caught);
            m.put("reveal", rev);
        } else {
            m.put("reveal", null);
        }
        return m;
    }
    private void startAuction(Room r, List<Player> acts) {
        int size = r.squadSize;
        String[] needs = formFor(size);
        Map<String, List<FifaCard>> pools = new LinkedHashMap<>();
        pools.put("GK", new ArrayList<FifaCard>());
        pools.put("DF", new ArrayList<FifaCard>());
        pools.put("MF", new ArrayList<FifaCard>());
        pools.put("FW", new ArrayList<FifaCard>());
        List<FifaCard> allCards = new ArrayList<>();
        for (Map<String, Object> pm : playerProfiles.values()) {
            FifaCard c = new FifaCard();
            c.n = s(pm.get("n"));
            c.e = s(pm.get("e"));
            c.pos = s(pm.get("pos"));
            c.t = s(pm.get("t"));
            c.c = s(pm.get("c"));
            c.r = i(pm.get("r"));
            c.v = i(pm.get("v"));
            c.num = i(pm.get("num"));
            c.price = 0;
            List<FifaCard> bucket = pools.get(c.pos);
            (bucket == null ? pools.get("MF") : bucket).add(c);
            allCards.add(c);
        }
        for (List<FifaCard> l : pools.values()) Collections.shuffle(l);

        // الديك: كل لاعب بياخد موقعه من كل مركز
        List<FifaCard> deck = new ArrayList<>();
        for (String slotPos : needs) {
            for (int idx = 0; idx < acts.size(); idx++) {
                List<FifaCard> bucket = pools.get(slotPos);
                FifaCard card = null;
                if (!bucket.isEmpty()) {
                    card = bucket.remove(0);
                } else {
                    for (List<FifaCard> b : pools.values()) {
                        if (!b.isEmpty()) { card = b.remove(0); break; }
                    }
                }
                if (card == null) break;
                deck.add(card);
            }
        }
        // المخزون = الكروت اللي فاضت
        List<FifaCard> spare = new ArrayList<>();
        for (List<FifaCard> l : pools.values()) spare.addAll(l);

        Auction a = new Auction();
        for (Player p : acts) {
            a.bidders.add(p.token);
            a.budgets.put(p.token, size == 11 ? 450 : 200);
            a.teams.put(p.token, new ArrayList<FifaCard>());
            a.stealUsed.put(p.token, false);
        }
        a.deck.addAll(deck);
        a.spare.addAll(spare);
        r.auction = a;
        r.auctionResult = null;
        r.winnerId = null;
        r.spiesWon = null;
        r.votePrompt = false;
        r.voteResponses.clear();
        r.voteOpen = false;
        r.voteTargets.clear();
        r.voteMap.clear();
        r.state = "playing";
        for (Player p : r.players.values()) {
            p.eliminated = !acts.contains(p);
            p.isSpy = false;
            p.charName = null;
            p.charEmoji = null;
            p.charProfile = null;
        }
        resetCard(r);
        chat(r, "🔨 مزاد النجوم بدأ! " + acts.size() + " لاعبين بينافسوا على " + deck.size()
                + " كارت (تشكيلة " + size + ") — الميزانية " + (size == 11 ? 450 : 200) + " مليون 💰");
    }

    private FifaCard currentCard(Room r) {
        Auction a = r.auction;
        if (a == null || a.cardIndex >= a.deck.size()) return null;
        return a.deck.get(a.cardIndex);
    }

    private int squadNeed(Room r) { return formFor(r.squadSize).length; }

    private boolean someoneNeedsCards(Room r) {
        Auction a = r.auction;
        int need = squadNeed(r);
        for (List<FifaCard> t : a.teams.values()) if (t.size() < need) return true;
        return false;
    }

    private void resetCard(Room r) {
        Auction a = r.auction;
        int need = squadNeed(r);
        // الكروت اللي محدش محتاجها (كل التشكيلات كملت) تتلحق للمخزون ونتخطاها
        while (currentCard(r) != null && !someoneNeedsCards(r)) {
            a.poolLeft.add(currentCard(r));
            a.cardIndex++;
        }
        FifaCard card = currentCard(r);
        a.currentBid = card == null ? 0 : cardBase(card);
        a.bidTurn = 0;
        a.needed = need;
        a.bidders.clear();
        for (Player p : actives(r)) if (a.teams.get(p.token).size() < need) a.bidders.add(p.token);
    }

    private void nextAuctionTurn(Room r) {
        Auction a = r.auction;
        if (a.bidders.isEmpty()) return;
        a.bidTurn = (a.bidTurn + 1) % a.bidders.size();
    }

    private void sellCardTo(Room r, String winnerId, int price, boolean unsold) {
        Auction a = r.auction;
        FifaCard card = currentCard(r);
        if (card == null) return;
        Player w = winnerId == null ? null : r.players.get(winnerId);
        if (unsold || w == null || price > a.budgets.getOrDefault(winnerId, -1)) {
            a.poolLeft.add(card);
            chat(r, "😴 ما حدش اشترى " + POS_EMOJI.getOrDefault(card.pos, "") + " " + card.n + " — الكارت راح للمخزون");
        } else {
            a.budgets.put(winnerId, a.budgets.getOrDefault(winnerId, 0) - price);
            card.price = price;
            a.teams.get(winnerId).add(card);
            chat(r, "🏆 " + w.name + " كسب " + POS_EMOJI.getOrDefault(card.pos, "") + " " + card.n + " بــ " + price + " مليون 💰");
        }
        a.cardIndex++;
        nextAuctionCard(r);
    }

    private void nextAuctionCard(Room r) {
        Auction a = r.auction;
        resetCard(r);
        if (a.cardIndex >= a.deck.size()) {
            fillTeams(r);
            a.phase = "steal";
            a.stealOrder.clear();
            for (Player p : actives(r)) a.stealOrder.add(p.token);
            a.stealTurn = 0;
            chat(r, "🔄 المزاد خلص! دلوقتي مرحلة الخطف — كل واحد ليه خطفة واحدة ⚡");
        }
    }

    private void fillTeams(Room r) {
        Auction a = r.auction;
        int need = squadNeed(r);
        for (List<FifaCard> team : a.teams.values()) {
            while (team.size() < need) {
                FifaCard card = a.poolLeft.isEmpty()
                        ? (a.spare.isEmpty() ? null : a.spare.remove(a.spare.size() - 1))
                        : a.poolLeft.remove(0);
                if (card == null) break;
                card.price = 0;
                team.add(card);
            }
        }
    }

    private int lowestCardOf(List<FifaCard> team) {
        int idx = 0;
        for (int i = 1; i < team.size(); i++) if (team.get(i).r < team.get(idx).r) idx = i;
        return idx;
    }

    private Map<String, Object> auctionAct(Room r, String token, Map<String, Object> body) {
        if (r == null || !"auction".equals(r.mode) || !"playing".equals(r.state)) return err("مش دلوقتي");
        Auction a = r.auction;
        if (a == null || !"bidding".equals(a.phase)) return err("المزايدة خلصت ✋");
        if (a.bidders.isEmpty()) return err("مفيش مزادين");
        if (a.cardIndex >= a.deck.size()) return err("المزاد خلص");
        FifaCard card = currentCard(r);
        Player me = r.players.get(token);
        if (me == null || me.eliminated) return err("مش بتلعب");
        if (!a.bidders.contains(token)) return err("مش في المزايدة");
        if (!token.equals(a.bidders.get(a.bidTurn % a.bidders.size()))) return err("مش دورك في المزايدة 🤨");
        String action = body == null ? null : s(body.get("action"));
        if ("buy".equals(action)) {
            int price = a.currentBid;
            if (price <= 0 || a.budgets.getOrDefault(token, -1) < price) return err("معندكش فلوس كفاية 😐");
            sellCardTo(r, token, price, false);
            return ok();
        }
        if ("raise".equals(action)) {
            int amount = 1;
            Object am = body == null ? null : body.get("amount");
            if (am instanceof Number) {
                int x = ((Number) am).intValue();
                if (x == 5 || x == 10) amount = x;
            }
            int next = a.currentBid + amount;
            if (a.budgets.getOrDefault(token, -1) < next) return err("الفلوس مش كفاية للزيادة دي 😐");
            a.currentBid = next;
            chat(r, "💸 " + me.name + " زايد على " + POS_EMOJI.getOrDefault(card.pos, "") + " " + card.n + " إلى " + next + " مليون");
            nextAuctionTurn(r);
            return ok();
        }
        if ("pass".equals(action)) {
            chat(r, "⏭️ " + me.name + " خرج من المزايدة على " + POS_EMOJI.getOrDefault(card.pos, "") + " " + card.n);
            a.bidders.remove(token);
            if (a.bidders.isEmpty()) {
                sellCardTo(r, null, 0, true);
            } else if (a.bidders.size() == 1) {
                a.bidTurn = 0;
                sellCardTo(r, a.bidders.get(0), a.currentBid, false);
            } else {
                if (a.bidTurn >= a.bidders.size()) a.bidTurn = 0;
            }
            return ok();
        }
        return err("إيه الحركة دي؟");
    }

    private Map<String, Object> stealAction(Room r, String token, Map<String, Object> body) {
        if (r == null || r.auction == null || !"steal".equals(r.auction.phase)) return err("مش في مرحلة الخطف");
        Auction a = r.auction;
        if (a.stealTurn >= a.stealOrder.size()) return err("الخطف خلص");
        if (!token.equals(a.stealOrder.get(a.stealTurn))) return err("مش دورك في الخطف 🤨");
        if (Boolean.TRUE.equals(a.stealUsed.get(token))) return err("استخدمت خطفتك خلاص");
        Player me = r.players.get(token);
        if (me == null) return err("invalid");
        Object st = body == null ? null : body.get("steal");
        if (!Boolean.TRUE.equals(st)) {
            a.stealUsed.put(token, true);
            chat(r, "⏭️ " + me.name + " اختار يعدي على الخطف");
        } else {
            String targetId = body == null ? null : s(body.get("targetId"));
            int cardIndex = -1;
            Object ci = body == null ? null : body.get("cardIndex");
            if (ci instanceof Number) cardIndex = ((Number) ci).intValue();
            Player target = targetId == null ? null : r.players.get(targetId);
            if (target == null || targetId.equals(token)) return err("هدف غير صالح");
            List<FifaCard> tTeam = a.teams.get(targetId);
            if (tTeam == null || cardIndex < 0 || cardIndex >= tTeam.size()) return err("اللاعب اللي اخترته مش موجود");
            List<FifaCard> myTeam = a.teams.get(token);
            if (myTeam == null || myTeam.isEmpty()) return err("تشكيلتك فاضية");
            int myLow = lowestCardOf(myTeam);
            FifaCard stolen = tTeam.remove(cardIndex);
            FifaCard given = myTeam.remove(myLow);
            tTeam.add(given);
            myTeam.add(stolen);
            a.stealUsed.put(token, true);
            chat(r, "⚡ " + me.name + " خطف " + POS_EMOJI.getOrDefault(stolen.pos, "") + " " + stolen.n
                    + " من " + target.name + " وسيب له " + given.n + "!");
        }
        a.stealTurn++;
        if (a.stealTurn >= a.stealOrder.size()) endAuction(r);
        return ok();
    }

    private void endAuction(Room r) {
        Auction a = r.auction;
        if (a == null) return;
        List<AuctionResult> sums = new ArrayList<>();
        for (Map.Entry<String, List<FifaCard>> e : a.teams.entrySet()) {
            AuctionResult ar = new AuctionResult();
            ar.id = e.getKey();
            ar.team.addAll(e.getValue());
            for (FifaCard c : ar.team) ar.total += c.r;
            sums.add(ar);
        }
        sums.sort((x, y) -> y.total - x.total);
        r.auctionResult = sums;
        r.winnerId = sums.isEmpty() ? null : sums.get(0).id;
        if (sums.size() >= 2) addPoints(r, sums.get(0).id, 3);
        if (sums.size() >= 4) addPoints(r, sums.get(1).id, 1);
        r.state = "over";
        Player winner = r.winnerId == null ? null : r.players.get(r.winnerId);
        int top = sums.isEmpty() ? 0 : sums.get(0).total;
        chat(r, winner != null ? "🏆 " + winner.name + " كسب المزاد بأعلى مجموع تقييم (" + top + ")" : "المزاد خلص");
        r.auction = null;
    }

    private void handleAuctionLeave(Room r, String token) {
        Auction a = r.auction;
        if (a == null) return;
        a.budgets.remove(token);
        a.teams.remove(token);
        a.stealUsed.remove(token);
        a.bidders.remove(token);
        a.stealOrder.remove(token);
        if (actives(r).size() < 2) { endAuction(r); return; }
        if ("bidding".equals(a.phase)) {
            if (a.bidders.isEmpty()) {
                sellCardTo(r, null, 0, true);
            } else if (a.bidders.size() == 1) {
                a.bidTurn = 0;
                sellCardTo(r, a.bidders.get(0), a.currentBid, false);
            } else {
                if (a.bidTurn >= a.bidders.size()) a.bidTurn = 0;
            }
        } else if ("steal".equals(a.phase)) {
            while (a.stealTurn < a.stealOrder.size()
                    && Boolean.TRUE.equals(a.stealUsed.get(a.stealOrder.get(a.stealTurn)))) a.stealTurn++;
            if (a.stealTurn >= a.stealOrder.size()) endAuction(r);
        }
    }

    /* ------------------- منطق التصويت (الجاسوس) 🗳️ ------------------- */
    private void addPoints(Room r, String token, int n) {
        r.points.put(token, r.points.getOrDefault(token, 0) + n);
    }

    private void chat(Room r, String text) {
        ChatMsg m = new ChatMsg();
        m.from = "النظام";
        m.system = true;
        m.text = text;
        m.ts = System.currentTimeMillis();
        r.chat.add(m);
    }

    private void tallyVotePrompt(Room r) {
        List<Player> acts = actives(r);
        int nowV = 0;
        for (Player p : acts) if ("now".equals(r.voteResponses.get(p.token))) nowV++;
        int laterV = acts.size() - nowV;
        r.votePrompt = false;
        r.voteResponses.clear();
        if (nowV > laterV) {
            r.voteOpen = true;
            r.voteTargets.clear();
            for (Player p : acts) r.voteTargets.add(p.token);
            r.voteMap.clear();
            chat(r, "🗳️ التصويت على الجاسوس بدأ! كل واحد يختار مين شاكك فيه — بالأغلبية 🎯");
        } else {
            chat(r, "📉 الأغلبية اختارت الاستمرار — الجولة بتكمّل 😤");
        }
    }

    private void resolveVote(Room r) {
        List<Player> acts = actives(r);
        Map<String, String> voteMap = new LinkedHashMap<>(r.voteMap);
        Map<String, Integer> tally = new LinkedHashMap<>();
        for (Player a : acts) {
            String t = voteMap.get(a.token);
            if (t != null) tally.put(t, tally.getOrDefault(t, 0) + 1);
        }
        r.voteOpen = false;
        r.voteTargets.clear();
        r.voteMap.clear();
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(tally.entrySet());
        sorted.sort((x, y) -> y.getValue() - x.getValue());
        if (sorted.isEmpty() || (sorted.size() > 1 && sorted.get(0).getValue().equals(sorted.get(1).getValue()))) {
            chat(r, "😅 التصويت اتعادل — ما حدش اتقبض، نكمّل اللعب");
            return;
        }
        Player accused = r.players.get(sorted.get(0).getKey());
        if (accused == null) return;
        if (accused.isSpy) {
            r.state = "over";
            r.spiesWon = false;
            for (Player a : acts) if (accused.token.equals(voteMap.get(a.token))) addPoints(r, a.token, 3);
            for (Player a : acts) {
                if (!a.token.equals(accused.token) && !accused.token.equals(voteMap.get(a.token)) && !a.isDoubleAgent) addPoints(r, a.token, 1);
            }
            chat(r, "🎉 التصويت قبض على الجاسوس! " + accused.name + " كان جاسوس 🕵️"
                    + (r.word != null ? " (الكلمة كانت: " + r.word[1] + " " + r.word[0] + ")" : ""));
            return;
        }
        StringBuilder wrongNames = new StringBuilder();
        for (Player a : acts) {
            if (accused.token.equals(voteMap.get(a.token))) {
                a.eliminated = true;
                if (wrongNames.length() > 0) wrongNames.append(" و");
                wrongNames.append(a.name);
            }
        }
        chat(r, "😅 الأغلبية اتهجمت على " + accused.name + " وطلع "
                + (accused.isDoubleAgent ? "عميل مزدوج! 🎭" : "مدني! ") + wrongNames + " اتقبضوا بدل الجاسوس");
        if (!checkSpyGameOver(r)) nextTurn(r);
    }

    private Map<String, Object> requestVote(Room r, String token) {
        if (!"spy".equals(r.mode) || !"playing".equals(r.state)) return err("مش في وقت تصويت");
        if (!r.hostToken.equals(token)) return err("صاحب الأوضة بس هو اللي يطلب");
        if (r.votePrompt || r.voteOpen) return err("فيه تصويت شغال خلاص");
        r.votePrompt = true;
        r.voteResponses.clear();
        r.voteResponses.put(token, "now"); // صاحب الأوضة مع «تصويت الآن» على طول
        chat(r, "🗳️ صاحب الأوضة طلب تصويت على الجاسوس! الكل يقرر: تصويت دلوقتي ولا نكمل؟");
        return ok();
    }

    private Map<String, Object> voteResponse(Room r, String token, boolean now) {
        if (!"spy".equals(r.mode) || !"playing".equals(r.state) || !r.votePrompt) return err("مش في وقت تصويت");
        if (r.hostToken.equals(token)) return err("حضرتك صاحب الأوضة — محسوب على طول");
        Player me = r.players.get(token);
        if (me == null || me.eliminated) return err("مش بتلعب");
        r.voteResponses.put(token, now ? "now" : "later");
        boolean all = true;
        for (Player p : actives(r)) {
            if (!r.voteResponses.containsKey(p.token)) { all = false; break; }
        }
        if (all) tallyVotePrompt(r);
        return ok();
    }

    private Map<String, Object> voteCast(Room r, String token, String targetId) {
        if (!r.voteOpen) return err("التصويت مقفول");
        Player me = r.players.get(token);
        if (me == null || me.eliminated) return err("مش بتلعب");
        if (targetId == null || token.equals(targetId) || !r.players.containsKey(targetId)) return err("هدف غير صالح");
        Player target = r.players.get(targetId);
        if (target == null || target.eliminated) return err("هدف غير صالح");
        r.voteMap.put(token, targetId);
        boolean all = true;
        for (Player p : actives(r)) {
            if (!r.voteMap.containsKey(p.token)) { all = false; break; }
        }
        if (all) resolveVote(r);
        return ok();
    }

    /* ------------------- عرض حالة المزاد/التصويت ------------------- */
    private Map<String, Object> cardJson(FifaCard c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("n", c.n);
        m.put("r", c.r);
        m.put("pos", c.pos);
        m.put("t", c.t);
        m.put("c", c.c);
        m.put("num", c.num);
        m.put("e", c.e);
        m.put("price", c.price);
        return m;
    }

    private List<Object> teamJson(List<FifaCard> team) {
        List<Object> out = new ArrayList<>();
        for (FifaCard c : team) out.add(cardJson(c));
        return out;
    }

    private Map<String, Object> auctionView(Room r, String token) {
        Auction a = r.auction;
        if (a == null) return null;
        FifaCard card = currentCard(r);
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("phase", a.phase);
        v.put("cardIndex", a.cardIndex);
        v.put("deckLength", a.deck.size());
        v.put("card", card == null ? null : cardJson(card));
        v.put("currentBid", a.currentBid);
        v.put("startBid", card == null ? 0 : cardBase(card));
        v.put("remainingBidders", a.bidders.size());
        v.put("iAmBidder", a.bidders.contains(token));
        v.put("bidTurnId", "bidding".equals(a.phase) && !a.bidders.isEmpty()
                ? a.bidders.get(a.bidTurn % a.bidders.size()) : null);
        v.put("myBudget", a.budgets.get(token));
        List<FifaCard> myTeam = a.teams.get(token);
        v.put("myTeam", teamJson(myTeam == null ? Collections.<FifaCard>emptyList() : myTeam));
        Map<String, Object> teams = new LinkedHashMap<>();
        for (Map.Entry<String, List<FifaCard>> e : a.teams.entrySet()) teams.put(e.getKey(), teamJson(e.getValue()));
        v.put("teams", teams);
        v.put("stealTurnId", "steal".equals(a.phase) && !a.stealOrder.isEmpty() && a.stealTurn < a.stealOrder.size()
                ? a.stealOrder.get(a.stealTurn) : null);
        v.put("stealUsed", new LinkedHashMap<String, Object>(a.stealUsed));
        v.put("canSteal", "steal".equals(a.phase) && a.stealTurn < a.stealOrder.size()
                ? (token.equals(a.stealOrder.get(a.stealTurn)) && !Boolean.TRUE.equals(a.stealUsed.get(token))) : false);
        return v;
    }

    private List<Object> auctionResultJson(List<AuctionResult> list) {
        List<Object> out = new ArrayList<>();
        if (list == null) return out;
        for (AuctionResult ar : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ar.id);
            m.put("total", ar.total);
            m.put("team", teamJson(ar.team));
            out.add(m);
        }
        return out;
    }

    private int i(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o == null ? "0" : o).trim()); } catch (Exception e) { return 0; }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Room r : new ArrayList<>(rooms.values())) {
            List<Player> gone = new ArrayList<>();
            for (Player p : r.players.values()) {
                if (now - p.lastSeen > 20_000) gone.add(p);
            }
            for (Player p : gone) leave(r, p);
            if (r.players.isEmpty()) rooms.remove(r.code);
        }
    }

    /* ------------------- حالة JSON ------------------- */
    @SuppressWarnings("unchecked")
    private Map<String, Object> stateFor(Room r, String token) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", r.code);
        m.put("host", r.hostToken);
        m.put("state", r.state);
        m.put("mode", r.mode);
        m.put("version", PROTOCOL_VERSION);
        m.put("categoryId", r.categoryId);
        m.put("difficulty", r.difficulty);
        m.put("categories", categoryInfo());
        m.put("spiesCount", r.spiesCount);
        m.put("doubleAgent", r.doubleAgent);
        m.put("spiesWon", r.spiesWon);
        if ("spy".equals(r.mode) && "over".equals(r.state) && r.word != null) {
            Map<String, Object> wm = new LinkedHashMap<>();
            wm.put("name", r.word[0]);
            wm.put("emoji", r.word[1]);
            m.put("word", wm);
        }
        m.put("myId", token);
        m.put("winnerId", r.winnerId);
        if ("playing".equals(r.state) && r.turnIndex < r.order.size()) {
            m.put("turnId", r.order.get(r.turnIndex));
        } else {
            m.put("turnId", null);
        }
        List<Object> players = new ArrayList<>();
        for (Player p : r.players.values()) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id", p.token);
            pm.put("name", p.name);
            pm.put("connected", true);
            pm.put("eliminated", p.eliminated);
            pm.put("isMe", p.token.equals(token));
            if (p.token.equals(token) || "over".equals(r.state)) {
                if (p.charName != null) {
                    Map<String, Object> cm = new LinkedHashMap<>();
                    cm.put("name", p.charName);
                    cm.put("emoji", p.charEmoji);
                    cm.put("isSpy", p.isSpy);
                    cm.put("isDoubleAgent", p.isDoubleAgent);
                    String im = charImageOf(p.charName, p.charProfile);
                    if (im != null) cm.put("img", im);
                    if (p.charProfile != null) {
                        cm.put("isPlayer", true);
                        cm.put("cc", jerseyColor(p.charProfile));
                        cm.put("profile", p.charProfile);
                    } else {
                        String bio = charBio.get(p.charName);
                        if (bio != null) cm.put("bio", bio);
                    }
                    pm.put("char", cm);
                }
            }
            players.add(pm);
        }
        m.put("players", players);
        List<Object> chat = new ArrayList<>();
        int from = Math.max(0, r.chat.size() - 60);
        for (int i = from; i < r.chat.size(); i++) {
            ChatMsg c = r.chat.get(i);
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("from", c.from);
            cm.put("text", c.text);
            cm.put("ts", c.ts);
            if (c.system) cm.put("system", true);
            if (c.mediaType != null) {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("type", c.mediaType);
                mm.put("data", c.mediaData);
                cm.put("media", mm);
            }
            chat.add(cm);
        }
        m.put("chat", chat);
        m.put("points", r.points);
        m.put("squadSize", r.squadSize);
        if (r.hostToken.equals(token)) {
            m.put("ips", getIps());
            if (!getIps().isEmpty()) m.put("hostIP", getIps().get(0));
        }
        // التصويت
        List<Player> acts = actives(r);
        m.put("votePrompt", r.votePrompt);
        m.put("myVoteResponse", r.votePrompt ? r.voteResponses.get(token) : null);
        if (r.votePrompt) {
            int responded = 0;
            for (Player p : acts) if (r.voteResponses.containsKey(p.token)) responded++;
            m.put("voteResponded", responded);
            m.put("voteTotal", acts.size());
        } else {
            m.put("voteResponded", 0);
            m.put("voteTotal", 0);
        }
        m.put("voteOpen", r.voteOpen);
        m.put("voteTargets", new ArrayList<String>(r.voteTargets));
        m.put("myVote", r.voteOpen ? r.voteMap.get(token) : null);
        if (r.voteOpen) {
            int cast = 0;
            for (Player p : acts) if (r.voteMap.containsKey(p.token)) cast++;
            m.put("voteCast", cast);
        } else {
            m.put("voteCast", 0);
        }
        // المزاد
        if ("auction".equals(r.mode)) {
            m.put("auction", auctionView(r, token));
            m.put("auctionResult", "over".equals(r.state) ? auctionResultJson(r.auctionResult) : null);
        } else {
            m.put("auction", null);
            m.put("auctionResult", null);
        }
        // مين الكذاب؟
        m.put("liar", "liar".equals(r.mode) ? liarView(r, token) : null);
        return m;
    }

    /* ------------------- HTTP ------------------- */
    private Response json(Object o) {
        Response r = newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8",
                MiniJson.write(o));
        r.addHeader("Cache-Control", "no-store");
        // إغلاق الاتصال بعد كل رد: نتحاشى أعطال الـ Keep-Alive
        // اللي بتعلّق الطلبات التالية في بعض متصفحات/ويبفيو الموبايل
        r.addHeader("Connection", "close");
        return r;
    }

    private Response error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", msg);
        return json(m);
    }

    private String mimeOf(String path) {
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".json")) return "application/json; charset=utf-8";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".webp")) return "image/webp";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "text/html; charset=utf-8";
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            /* ===== GET: ملفات ثابتة أو API ===== */
            if (Method.GET.equals(method)) {
                if (uri.startsWith("/api/")) return serveApiGet(session, uri);

                String file = uri.equals("/") ? "index.html" : uri.substring(1);
                InputStream in = null;
                try {
                    in = assets.open(file);
                } catch (IOException e) {
                    return error404();
                }
                Response r = newChunkedResponse(Response.Status.OK, mimeOf(file), in);
                r.addHeader("Cache-Control", "no-store");
                r.addHeader("Connection", "close");
                return r;
            }

            /* ===== POST: أفعال اللعبة ===== */
            if (Method.POST.equals(method) && uri.startsWith("/api/")) {
                String raw = readBodyUtf8(session);
                Map<String, Object> body = new LinkedHashMap<>();
                if (raw != null && !raw.isEmpty()) {
                    try {
                        body = MiniJson.obj(MiniJson.parse(raw));
                    } catch (Exception ignored) { }
                }
                return serveApiPost(uri, body);
            }

            return error404();
        } catch (Exception e) {
            return error("server error");
        }
    }

    private Response error404() {
        Response r = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain; charset=utf-8", "404");
        r.addHeader("Connection", "close");
        return r;
    }

    /**
     * قراءة جسم طلب POST بالبايتات وتفكيكه UTF-8 مباشرة.
     * نانوإتش‌تي‌بي‌دي بيحلل الجسم US-ASCII لو مفيش charset في الهيدر — فبنتجاوزه خالص.
     */
    private String readBodyUtf8(IHTTPSession session) {
        try {
            String cl = session.getHeaders().get("content-length");
            if (cl != null) {
                int len = Integer.parseInt(cl.trim());
                if (len <= 0) return null;
                if (len > 1024 * 1024) return null; // حماية من الضخامة
                byte[] buf = new byte[len];
                InputStream in = session.getInputStream();
                int off = 0;
                while (off < len) {
                    int r = in.read(buf, off, len - off);
                    if (r < 0) break;
                    off += r;
                }
                return off > 0 ? new String(buf, 0, off, StandardCharsets.UTF_8) : null;
            }
        } catch (Exception ignored) { }
        // استرجاع احتياطي لو مفيش content-length
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            return files.get("postData");
        } catch (Exception ignored) { return null; }
    }

    private Response serveApiGet(IHTTPSession session, String uri) {
        Map<String, String> parms = session.getParms();
        if ("/api/state".equals(uri)) {
            String token = parms.get("token");
            Room r = roomOf(token);
            if (r == null) return error("invalid");
            Player p = r.players.get(token);
            if (p == null) return error("invalid");
            p.lastSeen = System.currentTimeMillis();
            return json(stateFor(r, token));
        }
        if ("/api/info".equals(uri)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ips", getIps());
            m.put("port", currentPort);
            m.put("ok", true);
            return json(m);
        }
        return error404();
    }

    @SuppressWarnings("unchecked")
    private Response serveApiPost(String uri, Map<String, Object> body) {
        String name = s(body.get("name"));
        String code = s(body.get("code"));
        String token = s(body.get("token"));
        String text = s(body.get("text"));
        String targetId = s(body.get("targetId"));
        String guess = s(body.get("guess"));
        String mode = s(body.get("mode"));
        String categoryId = s(body.get("categoryId"));
        String count = s(body.get("count"));
        String size = s(body.get("size"));
        int v = 0;
        try { v = Integer.parseInt(s(body.get("v")) == null ? "0" : s(body.get("v"))); } catch (Exception ignored) { }
        boolean versionOk = v >= PROTOCOL_VERSION;

        switch (uri) {
            case "/api/create": {
                if (!versionOk) return json(err(VERSION_MESSAGE));
                Room r = new Room();
                if ("spy".equals(mode)) { r.mode = "spy"; r.categoryId = "sports"; }
                else if ("auction".equals(mode)) { r.mode = "auction"; }
                else if ("liar".equals(mode)) { r.mode = "liar"; }
                r.code = genCode(rooms);
                String t = newToken();
                Player p = new Player();
                p.token = t;
                p.name = blank(name, "لاعب");
                r.hostToken = t;
                r.players.put(t, p);
                rooms.put(r.code, r);
                tokenRoom.put(t, r.code);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ok", true);
                m.put("code", r.code);
                m.put("token", t);
                m.put("mode", r.mode);
                return json(m);
            }
            case "/api/set-category": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                if (!r.hostToken.equals(token)) return json(err("صاحب الأوضة بس هو اللي يحدد"));
                if ("playing".equals(r.state) || "over".equals(r.state)) return json(err("متحصلش التغيير بعد ما اللعب يبدأ"));
                if ("mix".equals(categoryId)) {
                    r.categoryId = "mix";
                    return json(ok());
                }
                for (Cat cat : CATS) {
                    if (cat.id.equals(categoryId)) { r.categoryId = categoryId; return json(ok()); }
                }
                return json(err("كاتيجوري مش موجودة"));
            }
            case "/api/set-difficulty": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                if (!r.hostToken.equals(token)) return json(err("صاحب الأوضة بس هو اللي يحدد الصعوبة"));
                if ("playing".equals(r.state) || "over".equals(r.state)) return json(err("متحصلش التغيير بعد ما اللعب يبدأ"));
                String d = s(body.get("difficulty"));
                if (isDiff(d)) {
                    r.difficulty = d;
                    return json(ok());
                }
                return json(err("صعوبة غير معروفة"));
            }
            case "/api/set-spy-count": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                if (!r.hostToken.equals(token)) return json(err("صاحب الأوضة بس هو اللي يحدد"));
                int n = 0;
                try { n = Integer.parseInt(count == null ? "" : count); } catch (Exception ignored) { }
                if (n == 1 || n == 2) r.spiesCount = n;
                return json(ok());
            }
            case "/api/set-double-agent": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                if (!r.hostToken.equals(token)) return json(err("صاحب الأوضة بس هو اللي يحدد"));
                Object onObj = body.get("on");
                r.doubleAgent = onObj instanceof Boolean && (Boolean) onObj;
                return json(ok());
            }
            case "/api/join": {
                if (!versionOk) return json(err(VERSION_MESSAGE));
                Room r = rooms.get((code == null ? "" : code).toUpperCase());
                if (r == null) return json(err("الكود غلط يا حيوان 🐒😄"));
                if (r.players.containsKey(token) && tokenRoom.containsKey(token)) {
                    return json(err("إنت داخل بالفعل"));
                }
                if ("playing".equals(r.state)) return json(err("اللعبة بدأت خلاص... استنى الجولة الجاية 😅"));
                if (r.players.size() >= 12) return json(err("الأوضة مليانة (12 لاعب)"));
                String t = newToken();
                Player p = new Player();
                p.token = t;
                p.name = blank(name, "لاعب");
                r.players.put(t, p);
                tokenRoom.put(t, r.code);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ok", true);
                m.put("code", r.code);
                m.put("token", t);
                return json(m);
            }
            case "/api/start": {
                String t = token == null ? "" : token;
                Room r = roomOf(t);
                if (r == null) return json(err("مش لاقيين الأوضة"));
                if (!r.hostToken.equals(t)) return json(err("صاحب الأوضة بس هو اللي يبدأ"));
                if (actives(r).size() < 2) return json(err("محتاجين على الأقل 2 لاعبين للعب 👥"));
                if ("spy".equals(r.mode) && actives(r).size() < 3)
                    return json(err("لعبة الجاسوس محتاجة 3 لاعبين على الأقل 🕵️"));
                if ("liar".equals(r.mode) && actives(r).size() < 3)
                    return json(err("لعبة مين الكذاب محتاجة 3 لاعبين على الأقل 🤥"));
                if (!attemptStart(r)) return json(err("الكاتيجوري صغيرة عن العدد ده، جرب كاتيجوري تاني 😐"));
                return json(ok());
            }
            case "/api/chat": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                Player p = r.players.get(token);
                if (p == null) return json(err("invalid"));
                text = text == null ? "" : text.trim();
                if (text.length() > 0 && text.length() <= 300) {
                    ChatMsg m = new ChatMsg();
                    m.from = p.name;
                    m.text = text;
                    m.ts = System.currentTimeMillis();
                    r.chat.add(m);
                }
                return json(ok());
            }
            case "/api/chat-media": { // ميديا في الشات (Feature 2)
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                Player p = r.players.get(token);
                if (p == null) return json(err("invalid"));
                Object mo = body.get("media");
                if (mo instanceof Map) {
                    Map<String, Object> mm = (Map<String, Object>) mo;
                    String type = s(mm.get("type"));
                    String data = s(mm.get("data"));
                    int limit = 0;
                    if ("image".equals(type)) limit = 700000;
                    else if ("audio".equals(type)) limit = 1500000;
                    else if ("video".equals(type)) limit = 3000000;
                    if (limit > 0 && data != null && data.startsWith("data:") && data.length() <= limit) {
                        ChatMsg m = new ChatMsg();
                        m.from = p.name;
                        m.ts = System.currentTimeMillis();
                        m.mediaType = type;
                        m.mediaData = data;
                        r.chat.add(m);
                    }
                }
                return json(ok());
            }
            case "/api/guess": {
                if (token != null) guess(token, targetId, guess);
                return json(ok());
            }
            case "/api/pass": {
                passTurn(token == null ? "" : token);
                return json(ok());
            }
            case "/api/next-round": {
                nextRound(token == null ? "" : token);
                return json(ok());
            }
            case "/api/set-squad-size": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                if (!r.hostToken.equals(token)) return json(err("صاحب الأوضة بس هو اللي يحدد"));
                int sz = 0;
                try { sz = Integer.parseInt(size == null ? "" : size); } catch (Exception ignored) { }
                if (sz == 5 || sz == 11) r.squadSize = sz;
                return json(ok());
            }
            case "/api/request-vote": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                return json(requestVote(r, token));
            }
            case "/api/vote-response": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                return json(voteResponse(r, token, Boolean.TRUE.equals(body.get("now"))));
            }
            case "/api/vote-cast": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                return json(voteCast(r, token, s(body.get("targetId"))));
            }
            case "/api/auction-act": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                return json(auctionAct(r, token, body));
            }
            case "/api/steal-act": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                return json(stealAction(r, token, body));
            }
            case "/api/liar-answer": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                Player p = r.players.get(token);
                if (p == null) return json(err("invalid"));
                liarAnswer(r, p, text);
                return json(ok());
            }
            case "/api/liar-start-vote": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                if (!r.hostToken.equals(token)) return json(err("صاحب الأوضة بس هو اللي يبدأ التصويت"));
                liarStartVote(r, token);
                return json(ok());
            }
            case "/api/liar-cast-vote": {
                Room r = roomOf(token);
                if (r == null) return json(err("invalid"));
                Player p = r.players.get(token);
                if (p == null) return json(err("invalid"));
                liarCastVote(r, p, s(body.get("targetId")));
                return json(ok());
            }
            case "/api/leave": {
                Room r = roomOf(token);
                if (r != null) leave(r, r.players.get(token));
                return json(ok());
            }
        }
        return error404();
    }

    private String s(Object o) { return o == null ? null : String.valueOf(o); }
    private String blank(String v, String def) {
        if (v == null) return def;
        v = v.trim();
        return v.isEmpty() ? def : v.substring(0, Math.min(20, v.length()));
    }
    private String newToken() { return UUID.randomUUID().toString().replace("-", ""); }
    private Map<String, Object> ok() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        return m;
    }
    private Map<String, Object> err(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", false);
        m.put("error", msg);
        return m;
    }
}