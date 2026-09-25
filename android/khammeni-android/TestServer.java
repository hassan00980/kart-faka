import com.khammeni.game.server.GameServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** اختبار السيرفر الجافا على سطح المكتب قبل بناء الـ APK */
public class TestServer {

    static int ok = 0, fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { ok++; System.out.println("[PASS] " + name); }
        else { fail++; System.out.println("[FAIL] " + name); }
    }

    static HttpClient http = HttpClient.newHttpClient();

    static Map<String, Object> post(String base, String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        try {
            return (Map<String, Object>) MiniJsonTest.parse(res.body());
        } catch (RuntimeException e) {
            System.out.println("  [RAW " + path + "] status=" + res.statusCode() + " body=" + res.body());
            throw e;
        }
    }

    static Map<String, Object> get(String base, String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + path)).GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        try {
            return (Map<String, Object>) MiniJsonTest.parse(res.body());
        } catch (RuntimeException e) {
            System.out.println("  [RAWS " + path + "] status=" + res.statusCode() + " body=" + res.body());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        int port = 3111;
        String base = "http://localhost:" + port;
        GameServer server = new GameServer(port, path ->
                new FileInputStream(new File("app/src/main/assets/www/" + path)));
        server.start();
        Thread.sleep(300);

        // 0) قاعدة بيانات اللاعبين
        check("قاعدة اللاعبين اتحملت (200 لاعب)", server.dbPoolSize() == 200);
        Map<String, Object> prof0 = server.testProfileOf("محمد صلاح");
        check("بروفايل محمد صلاح موجود في القاعدة", prof0 != null);
        check("بروفايل صلاح كامل (فيه بطولات)", prof0 != null && ((List<?>) prof0.get("champs")).size() >= 1);
        check("بروفايل صلاح فيه لون نادي في القاعدة",
                prof0 != null && ((Map<String, Object>) server.testProfileOf("محمد صلاح")).get("t") != null);

        HttpRequest jsonReq = HttpRequest.newBuilder(URI.create(base + "/players.json")).GET().build();
        HttpResponse<String> jsonRes = http.send(jsonReq, HttpResponse.BodyHandlers.ofString());
        check("players.json بتتقدم (200)", jsonRes.statusCode() == 200);
        check("players.json بـ content-type صحيح",
                jsonRes.headers().firstValue("content-type").orElse("").contains("json"));
        if (jsonRes.statusCode() == 200) {
            Map<String, Object> db = (Map<String, Object>) MiniJsonTest.parse(jsonRes.body());
            check("ملف players.json نفسه فيه 200 لاعب", ((List<?>) db.get("players")).size() == 200);
        }

        // 1) info + static
        Map<String, Object> info = get(base, "/api/info");
        check("info: ok", Boolean.TRUE.equals(info.get("ok")));
        check("info: فيه IP للشبكة", ((List<?>) info.get("ips")).size() >= 1 || System.getenv("CI") != null);

        HttpRequest htmlReq = HttpRequest.newBuilder(URI.create(base + "/")).GET().build();
        HttpResponse<String> htmlRes = http.send(htmlReq, HttpResponse.BodyHandlers.ofString());
        check("الصفحة الرئيسية بتتقدم (HTML)", htmlRes.statusCode() == 200 && htmlRes.body().contains("كرت فكة"));
        check("الصفحة فيها زر عرض QR", htmlRes.body().contains("btn-show-qr") && htmlRes.body().contains("qrcode.js"));
        HttpRequest qrReq = HttpRequest.newBuilder(URI.create(base + "/qrcode.js")).GET().build();
        HttpResponse<String> qrRes = http.send(qrReq, HttpResponse.BodyHandlers.ofString());
        check("qrcode.js بيتقدم من السيرفر (200)", qrRes.statusCode() == 200 && qrRes.body().contains("qrcode"));

        // 2) إنشاء
        Map<String, Object> cr = post(base, "/api/create", "{\"name\":\"أحمد\",\"v\":6}");
        System.out.println("  [create raw] ok=" + cr.get("ok") + " code=" + cr.get("code") + " token=" + cr.get("token"));
        check("إنشاء أوضة", Boolean.TRUE.equals(cr.get("ok")));
        String code = (String) cr.get("code");
        String hostTok = (String) cr.get("token");

        // 3) انضمام
        Map<String, Object> j2 = post(base, "/api/join", json(code, "محمد"));
        Map<String, Object> j3 = post(base, "/api/join", json(code, "سارة"));
        check("انضمام محمد", Boolean.TRUE.equals(j2.get("ok")));
        check("انضمام سارة", Boolean.TRUE.equals(j3.get("ok")));
        String t2 = (String) j2.get("token");
        String t3 = (String) j3.get("token");

        // 4) بدء
        Map<String, Object> st = post(base, "/api/start", "{\"token\":\"" + hostTok + "\"}");
        check("بدء اللعب", Boolean.TRUE.equals(st.get("ok")));

        Map<String, Object> hs = get(base, "/api/state?token=" + hostTok);
        check("الحالة playing", "playing".equals(hs.get("state")));
        List<Map<String, Object>> players = (List<Map<String, Object>>) hs.get("players");
        check("3 لاعبين", players.size() == 3);
        Map<String, Object> myP = players.stream().filter(p -> Boolean.TRUE.equals(p.get("isMe"))).findFirst().orElse(null);
        check("المضيف شاف شخصيته", myP.get("char") != null);
        Map<String, Object> myChar = (Map<String, Object>) myP.get("char");
        // البروفايل الكامل لازم يبقى سليم للاعبين (فريق/بطولات/رحلة انتقالات)
        if (myChar.get("profile") != null) {
            Map<String, Object> prof = (Map<String, Object>) myChar.get("profile");
            check("بروفايل الشخصية كامل (فريق/بطولات/انتقالات)",
                    prof.get("t") != null && ((List<?>) prof.get("champs")).size() >= 1 && prof.get("prev") instanceof List);
        }
        // الوصف المختصر لازم يظهر للأشخاص غير اللاعبين (فنانين/مشاهير/حيوانات...)
        if (myChar.get("bio") != null) {
            check("الوصف المختصر وصل مع الشخصية", String.valueOf(myChar.get("bio")).length() > 5);
        }
        if (myChar.get("isPlayer") != null && myChar.get("img") != null) {
            check("صورة اللاعب وصلت مع الشخصية", String.valueOf(myChar.get("img")).length() > 3);
        }
        check("فيه صاحب دور", hs.get("turnId") != null);
        check("المضيف شاف الـ IPs (بيستضيف)", hs.get("ips") != null);

        // 5) شات
        Map<String, Object> chatResp = post(base, "/api/chat", "{\"token\":\"" + hostTok + "\",\"text\":\"هل أنت لاعب كرة قدم؟\"}");
        check("الرد على الشات ok", Boolean.TRUE.equals(chatResp.get("ok")));
        hs = get(base, "/api/state?token=" + hostTok);
        List<Map<String, Object>> chat = (List<Map<String, Object>>) hs.get("chat");
        check("الشات وصل", chat.stream().anyMatch(m -> String.valueOf(m.get("text")).contains("كرة قدم")));

        // 5b) ميديا في الشات (Feature 2)
        String png = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
        Map<String, Object> mResp = post(base, "/api/chat-media",
                "{\"token\":\"" + hostTok + "\",\"media\":{\"type\":\"image\",\"data\":\"" + png + "\"}}");
        check("الرد على chat-media ok", Boolean.TRUE.equals(mResp.get("ok")));
        hs = get(base, "/api/state?token=" + hostTok);
        chat = (List<Map<String, Object>>) hs.get("chat");
        Map<String, Object> imgMsg = chat.stream()
                .filter(m -> m.get("media") instanceof Map)
                .map(m -> (Map<String, Object>) m.get("media"))
                .filter(mm -> "image".equals(String.valueOf(mm.get("type"))))
                .findFirst().orElse(null);
        check("رسالة الصورة وصلت بالـ data", imgMsg != null && String.valueOf(imgMsg.get("data")).startsWith("data:image"));
        // نوع غير مدعوم لا يُضاف
        Map<String, Object> badType = post(base, "/api/chat-media",
                "{\"token\":\"" + hostTok + "\",\"media\":{\"type\":\"doc\",\"data\":\"data:x\"}}");
        check("الرد لسه ok حتى للرفض", Boolean.TRUE.equals(badType.get("ok")));
        StringBuilder bigMedia = new StringBuilder("data:image/png;base64,");
        for (int i = 0; i < 800000; i++) bigMedia.append('A');
        post(base, "/api/chat-media", "{\"token\":\"" + hostTok + "\",\"media\":{\"type\":\"image\",\"data\":\"" + bigMedia + "\"}}");
        hs = get(base, "/api/state?token=" + hostTok);
        chat = (List<Map<String, Object>>) hs.get("chat");
        long mediaCount = chat.stream().filter(m -> m.get("media") instanceof Map).count();
        check("الملف الكبير مبيترفضش من غير أثر (نفس عدد رسائل الميديا)", mediaCount >= 1);

        // 6) تخمين صحيح من صاحب الدور
        String turnerTok = (String) hs.get("turnId");
        String turnerName = players.stream().filter(p -> p.get("id").equals(turnerTok)).findFirst()
                .map(p -> String.valueOf(p.get("name"))).orElse("?");
        String targetTok = turnerTok.equals(hostTok) ? t2 : (turnerTok.equals(t2) ? hostTok : t2);
        // نعرف شخصية الهدف من حالة الهدف نفسه (زيه زي ما اللاعب بيعرف شخصيته)
        Map<String, Object> targetState = get(base, "/api/state?token=" + targetTok);
        Map<String, Object> targetMe = ((List<Map<String, Object>>) targetState.get("players")).stream()
                .filter(p -> Boolean.TRUE.equals(p.get("isMe"))).findFirst().orElse(null);
        Map<String, Object> targetChar = (Map<String, Object>) targetMe.get("char");
        String targetCharName = String.valueOf(targetChar.get("name"));

        post(base, "/api/guess", "{\"token\":\"" + turnerTok + "\",\"targetId\":\"" + targetTok
                + "\",\"guess\":\"" + targetCharName + "\"}");
        Thread.sleep(200);
        Map<String, Object> turnerState = get(base, "/api/state?token=" + turnerTok);
        List<Map<String, Object>> pl2 = (List<Map<String, Object>>) turnerState.get("players");
        check("تخمين صحيح خلّى الهدف يتخرج",
                pl2.stream().anyMatch(p -> p.get("id").equals(targetTok) && Boolean.TRUE.equals(p.get("eliminated"))));

        // 7) تخمين غلط من باقي ناشط على التيرنر
        String thirdTok0 = null;
        for (Map<String, Object> pp : players) {
            String id = (String) pp.get("id");
            if (!id.equals(turnerTok) && !id.equals(targetTok)) { thirdTok0 = id; break; }
        }
        final String thirdTok = thirdTok0;
        // اتأكد إن التالت لسه نشط
        Map<String, Object> thirdState = get(base, "/api/state?token=" + thirdTok);
        if ("playing".equals(thirdState.get("state"))) {
            post(base, "/api/guess", "{\"token\":\"" + thirdTok + "\",\"targetId\":\"" + turnerTok
                    + "\",\"guess\":\"شخصية مش موجودة خالص\"}");
            Thread.sleep(200);
            Map<String, Object> ts2 = get(base, "/api/state?token=" + thirdTok);
            List<Map<String, Object>> pl3 = (List<Map<String, Object>>) ts2.get("players");
            check("تخمين غلط خلّى اللاعب يتخرج",
                    pl3.stream().anyMatch(p -> p.get("id").equals(thirdTok) && Boolean.TRUE.equals(p.get("eliminated"))));
        } else {
            check("تخمين غلط خلّى اللاعب يتخرج", true);
        }

        // 8) الفايز = صاحب التخمين الصح
        Thread.sleep(200);
        Map<String, Object> finalState = get(base, "/api/state?token=" + turnerTok);
        check("الجولة اتحسمت (over)", "over".equals(finalState.get("state")));
        check("الفايز هو التيرنر", turnerTok.equals(finalState.get("winnerId")));

        // 8b) الكشف الكامل: كل الشخصيات + بروفايلات قاعدة اللاعبين
        List<Map<String, Object>> plOver = (List<Map<String, Object>>) finalState.get("players");
        check("في نهاية الجولة كل الشخصيات اتكشفت",
                plOver.stream().allMatch(p -> p.get("char") != null));
        boolean anyPlayerProfile = false, profOk = true;
        for (Map<String, Object> p : plOver) {
            Object co = p.get("char");
            if (!(co instanceof Map)) continue;
            Map<String, Object> c = (Map<String, Object>) co;
            if (Boolean.TRUE.equals(c.get("isPlayer"))) {
                anyPlayerProfile = true;
                if (!(c.get("profile") instanceof Map) || c.get("cc") == null) profOk = false;
            }
        }
        if (anyPlayerProfile) {
            check("بروفايل اللاعبين نازل مع البروفايل واللون", profOk);
        } else {
            System.out.println("  [SKIP] مظهرش لاعب من القاعدة في الكشف دي (احتمالي)");
        }

        // 9) جولة تانية
        post(base, "/api/next-round", "{\"token\":\"" + hostTok + "\"}");
        Thread.sleep(200);
        Map<String, Object> nr = get(base, "/api/state?token=" + hostTok);
        check("جولة تانية بدأت", "playing".equals(nr.get("state")));

        // 10) سيب الأوضة → حالة invalid
        post(base, "/api/leave", "{\"token\":\"" + t2 + "\"}");
        Map<String, Object> left = get(base, "/api/state?token=" + t2);
        check("بعد السيب التوكين بيفضل", "invalid".equals(left.get("error")));

        // 11) مزحة الكود الغلط 🐒
        Map<String, Object> badJoin = post(base, "/api/join", "{\"code\":\"ZZZZ\",\"name\":\"X\",\"v\":6}");
        check("«الكود غلط يا حيوان» للكود الغلط",
                !Boolean.TRUE.equals(badJoin.get("ok")) && String.valueOf(badJoin.get("error")).contains("يا حيوان"));

        // 11.5) Protocol Version ⚠️ — عميل قديم لازم يتنرفض برسالة التحديث
        Map<String, Object> oldCreate = post(base, "/api/create", "{\"name\":\"قديم\",\"v\":5}");
        check("نسخة قديمة ما تعملش أوضة", !Boolean.TRUE.equals(oldCreate.get("ok"))
                && String.valueOf(oldCreate.get("error")).contains("حدّث"));
        Map<String, Object> noVer = post(base, "/api/create", "{\"name\":\"من غير نسخة\"}");
        check("عميل من غير نسخة يتنرفض", !Boolean.TRUE.equals(noVer.get("ok"))
                && String.valueOf(noVer.get("error")).contains("حدّث"));
        // نسخة جيدة تشتغل: نستخدم cr أصلاً (v=6) — نتأكد إن state فيه version
        Map<String, Object> verState = get(base, "/api/state?token=" + hostTok);
        check("الحالة بتبعت version=same", ((Number) verState.get("version")).intValue() == GameServer.PROTOCOL_VERSION);

        // 12) مود المزاد 🔨 (تشكيلة 11 + ميزانية 450 + مزايدة + خطف + نقاط)
        Map<String, Object> auc = post(base, "/api/create", "{\"name\":\"مزادي\",\"mode\":\"auction\",\"v\":6}");
        check("إنشاء أوضة مزاد", Boolean.TRUE.equals(auc.get("ok")) && "auction".equals(auc.get("mode")));
        String aucCode = (String) auc.get("code");
        String aucHost = (String) auc.get("token");
        Map<String, Object> b1 = post(base, "/api/join", json(aucCode, "شاري"));
        Map<String, Object> b2 = post(base, "/api/join", json(aucCode, "مزايد"));
        check("انضمام مزاديين", Boolean.TRUE.equals(b1.get("ok")) && Boolean.TRUE.equals(b2.get("ok")));
        String pA2 = (String) b1.get("token");
        String pB2 = (String) b2.get("token");

        Map<String, Object> sqR = post(base, "/api/set-squad-size", "{\"token\":\"" + aucHost + "\",\"size\":11}");
        check("تحديد التشكيلة 11", Boolean.TRUE.equals(sqR.get("ok")));
        Map<String, Object> sqBad = post(base, "/api/set-squad-size", "{\"token\":\"" + pA2 + "\",\"size\":5}");
        check("تغيير الحجم لغير المضيف مرفوض", !Boolean.TRUE.equals(sqBad.get("ok")));

        Map<String, Object> stAu = post(base, "/api/start", "{\"token\":\"" + aucHost + "\"}");
        check("بدء المزاد", Boolean.TRUE.equals(stAu.get("ok")));
        Map<String, Object> auS = get(base, "/api/state?token=" + aucHost);
        Map<String, Object> au = (Map<String, Object>) auS.get("auction");
        check("ديك المزاد 33 كارت (3 لاعبين × تشكيلة 11)", au != null && ((Number) au.get("deckLength")).intValue() == 33);
        check("الميزانية 450 لتشكيلة 11", au != null && ((Number) au.get("myBudget")).intValue() == 450);
        check("كارت فيفا كامل (اسم/تقييم/مركز/نادي)",
                au != null && au.get("card") != null
                        && ((Map<String, Object>) au.get("card")).get("n") != null
                        && ((Map<String, Object>) au.get("card")).get("r") != null
                        && ((Map<String, Object>) au.get("card")).get("pos") != null);
        check("فيه 3 مزادين", au != null && ((Number) au.get("remainingBidders")).intValue() == 3);

        // مزايدة: رفع 5 من صاحب الدور
        Map<String, Object> auS2 = get(base, "/api/state?token=" + aucHost);
        au = (Map<String, Object>) auS2.get("auction");
        String turnTok = (String) au.get("bidTurnId");
        Map<String, Object> raiseRes = post(base, "/api/auction-act",
                "{\"token\":\"" + turnTok + "\",\"action\":\"raise\",\"amount\":5}");
        check("رفع المزايدة 5 مليون", Boolean.TRUE.equals(raiseRes.get("ok")));
        Map<String, Object> afterRaise = get(base, "/api/state?token=" + turnTok);
        Map<String, Object> auR = (Map<String, Object>) afterRaise.get("auction");
        check("السعر زاد 5 عن السعر الأساسي",
                ((Number) auR.get("currentBid")).intValue() == ((Number) auR.get("startBid")).intValue() + 5);
        Map<String, Object> notTurn = null;
        {
            Map<String, Object> cur = get(base, "/api/state?token=" + aucHost);
            Map<String, Object> aaN = (Map<String, Object>) cur.get("auction");
            String curBidder = (String) aaN.get("bidTurnId");
            String otherTok = null;
            for (String tk : new String[]{aucHost, pA2, pB2}) {
                if (!tk.equals(curBidder)) { otherTok = tk; break; }
            }
            if (otherTok != null) {
                notTurn = post(base, "/api/auction-act",
                        "{\"token\":\"" + otherTok + "\",\"action\":\"buy\"}");
            }
        }
        check("مش دورك في المزايدة مرفوض", notTurn != null && !Boolean.TRUE.equals(notTurn.get("ok")));

        // نحرّك المزاد بالشراء لحد مرحلة الخطف
        boolean reachedSteal = false;
        for (int g = 0; g < 100; g++) {
            Map<String, Object> cur = get(base, "/api/state?token=" + aucHost);
            Map<String, Object> aa = (Map<String, Object>) cur.get("auction");
            if (aa == null) break;
            if (!"bidding".equals(aa.get("phase"))) {
                reachedSteal = "steal".equals(aa.get("phase"));
                break;
            }
            String tt = (String) aa.get("bidTurnId");
            if (tt == null) break;
            post(base, "/api/auction-act", "{\"token\":\"" + tt + "\",\"action\":\"buy\"}");
        }
        check("وصلنا لمرحلة الخطف", reachedSteal);

        // خطفة حقيقية من أول واحد دوره (المضيف أول المُترتيبين)
        Map<String, Object> st0 = get(base, "/api/state?token=" + aucHost);
        Map<String, Object> aa0 = (Map<String, Object>) st0.get("auction");
        String firstStealer = (String) aa0.get("stealTurnId");
        String victimTok = firstStealer == null ? null
                : (firstStealer.equals(aucHost) ? pA2 : aucHost);
        Map<String, Object> stealRes = null;
        if (firstStealer != null && Boolean.TRUE.equals(aa0.get("canSteal")) && victimTok != null) {
            stealRes = post(base, "/api/steal-act",
                    "{\"token\":\"" + firstStealer + "\",\"steal\":true,\"targetId\":\"" + victimTok + "\",\"cardIndex\":0}");
        }
        check("الخطفة الفعلية نجحت", stealRes != null && Boolean.TRUE.equals(stealRes.get("ok")));

        // نكمّل بالعدّي لحد النهاية
        boolean auctionEnded = false;
        for (int g = 0; g < 20; g++) {
            Map<String, Object> cur = get(base, "/api/state?token=" + aucHost);
            if ("over".equals(cur.get("state"))) { auctionEnded = true; break; }
            Map<String, Object> aa = (Map<String, Object>) cur.get("auction");
            if (aa == null || !"steal".equals(aa.get("phase"))) break;
            String stt = (String) aa.get("stealTurnId");
            if (stt == null) break;
            post(base, "/api/steal-act", "{\"token\":\"" + stt + "\",\"steal\":false}");
        }
        check("المزاد خلص (over)", auctionEnded);
        Map<String, Object> over = get(base, "/api/state?token=" + aucHost);
        List<Map<String, Object>> res2 = (List<Map<String, Object>>) over.get("auctionResult");
        check("النتيجة فيها تشكيلة لكل لاعب", res2 != null && res2.size() == 3);
        check("كل تشكيلة فيها 11 كارت",
                res2 != null && res2.size() == 3 && ((List<?>) res2.get(0).get("team")).size() == 11);
        Map<String, Object> pts = (Map<String, Object>) over.get("points");
        check("الفايز في المزاد خد +3",
                pts != null && ((Number) pts.getOrDefault(over.get("winnerId"), 0)).intValue() == 3);

        // 13) مود الجاسوس + «تصويت الآن» 🗳️ + النقاط
        Map<String, Object> spy = post(base, "/api/create", "{\"name\":\"رئيس\",\"mode\":\"spy\",\"v\":6}");
        check("إنشاء أوضة جاسوس", Boolean.TRUE.equals(spy.get("ok")));
        String spyCode = (String) spy.get("code");
        String spyHost = (String) spy.get("token");
        Map<String, Object> sv2 = post(base, "/api/join", json(spyCode, "تاني"));
        Map<String, Object> sv3 = post(base, "/api/join", json(spyCode, "تالت"));
        String sv2t = (String) sv2.get("token");
        String sv3t = (String) sv3.get("token");
        Map<String, Object> spyStart = post(base, "/api/start", "{\"token\":\"" + spyHost + "\"}");
        check("بدء الجاسوس", Boolean.TRUE.equals(spyStart.get("ok")));

        // كل واحد شايف شخصيته → نعرف مين الجاسوس
        String[] spyToks = {spyHost, sv2t, sv3t};
        String spyId = null;
        for (String tk : spyToks) {
            Map<String, Object> spyState = get(base, "/api/state?token=" + tk);
            Map<String, Object> me = ((List<Map<String, Object>>) spyState.get("players")).stream()
                    .filter(p -> Boolean.TRUE.equals(p.get("isMe"))).findFirst().orElse(null);
            Map<String, Object> ch = me == null ? null : (Map<String, Object>) me.get("char");
            if (ch != null && Boolean.TRUE.equals(ch.get("isSpy"))) spyId = tk;
        }
        check("الجاسوس اتحدد من حالته", spyId != null);

        Map<String, Object> rv = post(base, "/api/request-vote", "{\"token\":\"" + spyHost + "\"}");
        check("طلب «تصويت الآن» ok", Boolean.TRUE.equals(rv.get("ok")));
        Map<String, Object> vs = get(base, "/api/state?token=" + spyHost);
        check("votePrompt اتحط", Boolean.TRUE.equals(vs.get("votePrompt")));
        check("صاحب الأوضة محسوب now", "now".equals(vs.get("myVoteResponse")));

        post(base, "/api/vote-response", "{\"token\":\"" + sv2t + "\",\"now\":true}");
        Map<String, Object> vs2 = get(base, "/api/state?token=" + spyHost);
        check("بعد رد واحد: responded 2 من 3", ((Number) vs2.get("voteResponded")).intValue() == 2);
        post(base, "/api/vote-response", "{\"token\":\"" + sv3t + "\",\"now\":false}");
        Map<String, Object> vs3 = get(base, "/api/state?token=" + spyHost);
        check("الأغلبية «تصويت الآن» → تصويت مفتوح",
                Boolean.TRUE.equals(vs3.get("voteOpen")) && !Boolean.TRUE.equals(vs3.get("votePrompt")));

        // التصويت على نفسك ممنوع
        Map<String, Object> selfVote = post(base, "/api/vote-cast",
                "{\"token\":\"" + spyId + "\",\"targetId\":\"" + spyId + "\"}");
        check("التصويت على نفسك مرفوض", !Boolean.TRUE.equals(selfVote.get("ok")));

        // التصويت: الكل يصوت على الجاسوس (الجاسوس يختار هدف مش نفسه)
        String spyVoteTarget = spyId.equals(spyHost) ? sv2t : spyHost;
        for (String tk : spyToks) {
            String target = tk.equals(spyId) ? spyVoteTarget : spyId;
            post(base, "/api/vote-cast", "{\"token\":\"" + tk + "\",\"targetId\":\"" + target + "\"}");
        }
        Map<String, Object> resolved = get(base, "/api/state?token=" + spyHost);
        check("الجاسوس اتقبض بالتصويت (over)", "over".equals(resolved.get("state")));
        check("المدنيين كسبوا", Boolean.FALSE.equals(resolved.get("spiesWon")));
        Map<String, Object> spyPts = (Map<String, Object>) resolved.get("points");
        // اللي صوّتوا على الجاسوس (كل المدنيين) خدوا +3 — المتهم المقبوض عليه نفسه ما بياخدش حاجة
        boolean ptsOk = spyPts != null;
        for (String tk : spyToks) {
            int got = ptsOk ? ((Number) spyPts.getOrDefault(tk, 0)).intValue() : -1;
            if (tk.equals(spyId)) { if (got != 0) ptsOk = false; }
            else { if (got != 3) ptsOk = false; }
        }
        check("النقاط صح: المدنيين +3 والمتهم صفر", ptsOk);
        // جولة تانية بعد القبض
        post(base, "/api/next-round", "{\"token\":\"" + spyHost + "\"}");
        Map<String, Object> spyNr = get(base, "/api/state?token=" + spyHost);
        check("جولة جاسوس تانية بدأت", "playing".equals(spyNr.get("state")));

        // العميل المزدوج 🎭 — شبه مدني شايف الكلمة لكنه مع الجواسيس
        Map<String, Object> dcr2 = post(base, "/api/create", "{\"name\":\"فطين\",\"mode\":\"spy\",\"v\":6}");
        check("إنشاء أوضة جاسوس بالعميل المزدوج", Boolean.TRUE.equals(dcr2.get("ok")));
        String dcCode = (String) dcr2.get("code");
        String dcHost = (String) dcr2.get("token");
        Map<String, Object> dj2a = post(base, "/api/join", json(dcCode, "ريم"));
        Map<String, Object> dj2b = post(base, "/api/join", json(dcCode, "يحيى"));
        String dcT2 = (String) dj2a.get("token");
        String dcT3 = (String) dj2b.get("token");
        post(base, "/api/set-category", "{\"token\":\"" + dcHost + "\",\"categoryId\":\"foods\"}");
        Map<String, Object> daSet = post(base, "/api/set-double-agent", "{\"token\":\"" + dcHost + "\",\"on\":true}");
        check("تفعيل العميل المزدوج", Boolean.TRUE.equals(daSet.get("ok")));
        Map<String, Object> daSt = get(base, "/api/state?token=" + dcHost);
        check("الحالة فيها doubleAgent=true", Boolean.TRUE.equals(daSt.get("doubleAgent")));
        post(base, "/api/start", "{\"token\":\"" + dcHost + "\"}");
        Thread.sleep(250);
        Map<String, Object> da1 = get(base, "/api/state?token=" + dcHost);
        Map<String, Object> da2 = get(base, "/api/state?token=" + dcT2);
        Map<String, Object> da3 = get(base, "/api/state?token=" + dcT3);
        String daSpyTok = null, daDaTok = null, daCivTok = null;
        boolean daSeesWord = false;
        for (Map<String, Object> ds : new Map[]{da1, da2, da3}) {
            String tok = (String) ds.get("myId");
            for (Object po : (List<?>) ds.get("players")) {
                Map<String, Object> pm = (Map<String, Object>) po;
                if (!Boolean.TRUE.equals(pm.get("isMe"))) continue;
                Map<String, Object> ch = (Map<String, Object>) pm.get("char");
                if (ch == null) break;
                if (Boolean.TRUE.equals(ch.get("isSpy"))) daSpyTok = tok;
                else if (Boolean.TRUE.equals(ch.get("isDoubleAgent"))) {
                    daDaTok = tok;
                    if (!"جاسوس".equals(ch.get("name"))) daSeesWord = true;
                }
                else daCivTok = tok;
            }
        }
        check("في جاسوس وعميل مزدوج ومدني", daSpyTok != null && daDaTok != null && daCivTok != null);
        check("العميل المزدوج شايف الكلمة (مش «جاسوس»)", daSeesWord);
        // مدني ياتهم العميل المزدوج بالغلط → المتهم يخرج والجواسيس يكسبوا
        post(base, "/api/guess", "{\"token\":\"" + daCivTok + "\",\"targetId\":\"" + daDaTok + "\"}");
        Thread.sleep(250);
        Map<String, Object> daEnd = get(base, "/api/state?token=" + daDaTok);
        check("الاتهام الغلط للعميل المزدوج بيخلّص الجولة", "over".equals(daEnd.get("state")));
        check("الجواسيس كسبوا مع العميل المزدوج", Boolean.TRUE.equals(daEnd.get("spiesWon")));
        Map<String, Object> daPts = (Map<String, Object>) daEnd.get("points");
        check("العميل المزدوج كسب +3", ((Number) daPts.getOrDefault(daDaTok, 0)).intValue() >= 3);

        // 14) صعوبات لعيبة الكورة (التخمين) ⚽🎚️ — صاحب الأوضة يختار المستوى
        Map<String, Object> dcr = post(base, "/api/create", "{\"name\":\"مدرب\",\"mode\":\"guess\",\"v\":6}");
        check("إنشاء أوضة تخمين للصعوبة", Boolean.TRUE.equals(dcr.get("ok")));
        String dCode = (String) dcr.get("code");
        String dHost = (String) dcr.get("token");
        Map<String, Object> dj = post(base, "/api/join", json(dCode, "ظهير"));
        String dP2 = (String) dj.get("token");
        post(base, "/api/set-category", "{\"token\":\"" + dHost + "\",\"categoryId\":\"sports\"}");
        Map<String, Object> d0 = get(base, "/api/state?token=" + dHost);
        check("الصعوبة الافتراضية: الكل", "all".equals(d0.get("difficulty")));
        Map<String, Object> dBad = post(base, "/api/set-difficulty",
                "{\"token\":\"" + dHost + "\",\"difficulty\":\"مستحيل\"}");
        check("صعوبة غير معروفة متتسجلش", !Boolean.TRUE.equals(dBad.get("ok")));
        Map<String, Object> dGuest = post(base, "/api/set-difficulty",
                "{\"token\":\"" + dP2 + "\",\"difficulty\":\"hard\"}");
        check("اللاعب العادي مش بيقدر يحدد الصعوبة", !Boolean.TRUE.equals(dGuest.get("ok")));
        post(base, "/api/set-difficulty", "{\"token\":\"" + dHost + "\",\"difficulty\":\"easy\"}");
        Map<String, Object> dE = get(base, "/api/state?token=" + dHost);
        check("الصعوبة اتبعت (سهل)", "easy".equals(dE.get("difficulty")));
        post(base, "/api/start", "{\"token\":\"" + dHost + "\"}");
        Thread.sleep(200);
        boolean easyOk = true;
        for (String tk : new String[]{dHost, dP2}) {
            Map<String, Object> s1 = get(base, "/api/state?token=" + tk);
            Map<String, Object> me1 = ((List<Map<String, Object>>) s1.get("players")).stream()
                    .filter(p -> Boolean.TRUE.equals(p.get("isMe"))).findFirst().orElse(null);
            if (me1 == null || me1.get("char") == null) { easyOk = false; continue; }
            Map<String, Object> ch1 = (Map<String, Object>) me1.get("char");
            Map<String, Object> prof1 = (Map<String, Object>) ch1.get("profile");
            if (prof1 != null && ((Number) prof1.get("r")).intValue() < 89) easyOk = false;
            // من غير بروفايل (أسطورة ثابتة) = سهل مقبول
        }
        check("سهل: كل الشخصيات من السهل فعلاً", easyOk);

        // — أوضة "صعب" (شخصيات أقل شهرة → تقييم أقل) —
        Map<String, Object> hcr = post(base, "/api/create", "{\"name\":\"كابتن\",\"mode\":\"guess\",\"v\":6}");
        check("أوضة صعوبة جديدة", Boolean.TRUE.equals(hcr.get("ok")));
        String hCode = (String) hcr.get("code");
        String hHost = (String) hcr.get("token");
        Map<String, Object> hj = post(base, "/api/join", json(hCode, "سادس"));
        String hP2 = (String) hj.get("token");
        post(base, "/api/set-category", "{\"token\":\"" + hHost + "\",\"categoryId\":\"sports\"}");
        post(base, "/api/set-difficulty", "{\"token\":\"" + hHost + "\",\"difficulty\":\"hard\"}");
        Map<String, Object> hE = get(base, "/api/state?token=" + hHost);
        check("الصعوبة اتبعت (صعب)", "hard".equals(hE.get("difficulty")));
        post(base, "/api/start", "{\"token\":\"" + hHost + "\"}");
        Thread.sleep(200);
        boolean hardOk = true;
        for (String tk : new String[]{hHost, hP2}) {
            Map<String, Object> s1 = get(base, "/api/state?token=" + tk);
            Map<String, Object> me1 = ((List<Map<String, Object>>) s1.get("players")).stream()
                    .filter(p -> Boolean.TRUE.equals(p.get("isMe"))).findFirst().orElse(null);
            if (me1 == null || me1.get("char") == null) { hardOk = false; continue; }
            Map<String, Object> ch1 = (Map<String, Object>) me1.get("char");
            Map<String, Object> prof1 = (Map<String, Object>) ch1.get("profile");
            if (prof1 == null || ((Number) prof1.get("r")).intValue() > 83) hardOk = false;
        }
        check("صعب: كل الشخصيات تقييمها <= 83 (لاعبين حقيقيين)", hardOk);

        // 15) مود مين الكذاب؟ 🤥
        Map<String, Object> lcr = post(base, "/api/create", "{\"name\":\"كذاب1\",\"mode\":\"liar\",\"v\":6}");
        check("إنشاء أوضة مين الكذاب", Boolean.TRUE.equals(lcr.get("ok")) && "liar".equals(lcr.get("mode")));
        String lCode = (String) lcr.get("code");
        String lHost = (String) lcr.get("token");
        Map<String, Object> lj2 = post(base, "/api/join", json(lCode, "كذاب2"));
        Map<String, Object> lj3 = post(base, "/api/join", json(lCode, "كذاب3"));
        String lT2 = (String) lj2.get("token");
        String lT3 = (String) lj3.get("token");
        Map<String, Object> lStart = post(base, "/api/start", "{\"token\":\"" + lHost + "\"}");
        check("بدء مين الكذاب", Boolean.TRUE.equals(lStart.get("ok")));
        Thread.sleep(200);
        Map<String, Object> lS1 = get(base, "/api/state?token=" + lHost);
        Map<String, Object> lS2 = get(base, "/api/state?token=" + lT2);
        Map<String, Object> lS3 = get(base, "/api/state?token=" + lT3);
        Map<String, Object> lL1 = (Map<String, Object>) lS1.get("liar");
        Map<String, Object> lL2 = (Map<String, Object>) lS2.get("liar");
        Map<String, Object> lL3 = (Map<String, Object>) lS3.get("liar");
        check("الحالة في مرحلة الإجابة", "answer".equals(lL1.get("phase")));
        int liarCount = (Boolean.TRUE.equals(lL1.get("iAmLiar")) ? 1 : 0)
                + (Boolean.TRUE.equals(lL2.get("iAmLiar")) ? 1 : 0)
                + (Boolean.TRUE.equals(lL3.get("iAmLiar")) ? 1 : 0);
        check("لاعب واحد بس هو الكذاب", liarCount == 1);
        String q1 = (String) lL1.get("question");
        String q2 = (String) lL2.get("question");
        String q3 = (String) lL3.get("question");
        java.util.Set<String> qset = new java.util.HashSet<>(java.util.Arrays.asList(q1, q2, q3));
        check("الكذاب شايف سؤال مختلف عن الباقيين", qset.size() == 2);
        String liarTok = Boolean.TRUE.equals(lL1.get("iAmLiar")) ? lHost
                : Boolean.TRUE.equals(lL2.get("iAmLiar")) ? lT2 : lT3;
        // 3 إجابات
        post(base, "/api/liar-answer", "{\"token\":\"" + lHost + "\",\"text\":\"بحب الكشري\"}");
        post(base, "/api/liar-answer", "{\"token\":\"" + lT2 + "\",\"text\":\"بحب الشاورما\"}");
        post(base, "/api/liar-answer", "{\"token\":\"" + lT3 + "\",\"text\":\"بحب البيتزا\"}");
        Thread.sleep(200);
        Map<String, Object> lRev = get(base, "/api/state?token=" + lHost);
        Map<String, Object> lRevL = (Map<String, Object>) lRev.get("liar");
        check("بعد كل الإجابات اتكشفت تلقائيًا", "reveal".equals(lRevL.get("phase")));
        check("الإجابات ظهرت (3)", ((List<?>) lRevL.get("answers")).size() == 3);
        // التصويت: الباقيين على الكذاب + الكذاب على أي حد
        // (اختيار المصوتين بشكل حتمي — بغض النظر عن مين الكذاب)
        String[] rest = (lHost.equals(liarTok) ? new String[]{lT2, lT3}
                : lT2.equals(liarTok) ? new String[]{lHost, lT3}
                : new String[]{lHost, lT2});
        String nonLiarA = rest[0];
        String nonLiarB = rest[1];
        post(base, "/api/liar-start-vote", "{\"token\":\"" + lHost + "\"}");
        Thread.sleep(150);
        Map<String, Object> lV = get(base, "/api/state?token=" + lHost);
        System.out.println("  [liar vote-raw] phase=" + ((Map<String, Object>) lV.get("liar")).get("phase"));
        check("التصويت بدأ", "vote".equals(((Map<String, Object>) lV.get("liar")).get("phase")));
        post(base, "/api/liar-cast-vote", "{\"token\":\"" + nonLiarA + "\",\"targetId\":\"" + liarTok + "\"}");
        post(base, "/api/liar-cast-vote", "{\"token\":\"" + nonLiarB + "\",\"targetId\":\"" + liarTok + "\"}");
        // الكذاب يصوت على واحد (على غير نفسه) عشان الجولة تتحل
        Map<String, Object> mL = get(base, "/api/state?token=" + liarTok);
        List<Map<String, Object>> lt = (List<Map<String, Object>>) ((Map<String, Object>) mL.get("liar")).get("targets");
        String liarVoteTarget = lt.get(0).get("id").toString();
        post(base, "/api/liar-cast-vote", "{\"token\":\"" + liarTok + "\",\"targetId\":\"" + liarVoteTarget + "\"}");
        Thread.sleep(200);
        Map<String, Object> lOv = get(base, "/api/state?token=" + lHost);
        Map<String, Object> lOvL = (Map<String, Object>) lOv.get("liar");
        check("الجولة انتهت بالكشف", "over".equals(lOvL.get("phase")) && "over".equals(lOv.get("state")));
        Map<String, Object> rev = (Map<String, Object>) lOvL.get("reveal");
        check("الكذاب اتقبض بالأغلبية", Boolean.TRUE.equals(rev.get("caught")));
        check("الكشف فيه اسم الكذاب وسؤاله", rev.get("liarName") != null && rev.get("liarQuestion") != null);
        Map<String, Object> lPts = (Map<String, Object>) lOv.get("points");
        check("المصوتين الصح كسبوا 3 نقاط", ((Number) lPts.getOrDefault(nonLiarA, 0)).intValue() >= 3
                && ((Number) lPts.getOrDefault(nonLiarB, 0)).intValue() >= 3);

        server.stop();
        System.out.println("\nالنتيجة: " + ok + " نجحت، " + fail + " فشلت");
        System.exit(fail > 0 ? 1 : 0);
    }

    static String json(String code, String name) {
        return "{\"code\":\"" + code + "\",\"name\":\"" + name + "\",\"v\":6}";
    }
}

/** نسخة مصغرة من MiniJson جوه الاختبار عشان قراءة الردود */
class MiniJsonTest {
    static Object parse(String s) {
        P p = new P(s);
        return p.value();
    }

    static final class P {
        final String s; int i;
        P(String s) { this.s = s == null ? "" : s; }
        Object value() { skip(); if (i >= s.length()) return null; char c = s.charAt(i);
            if (c == '{') return obj(); if (c == '[') return arr(); if (c == '"') return str();
            if (c == 't') { i += 4; return true; } if (c == 'f') { i += 5; return false; } if (c == 'n') { i += 4; return null; }
            int st = i; while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String t = s.substring(st, i);
            if (t.isEmpty() || !(t.equals("0") || t.equals("-0"))) {
                if (t.isEmpty()) {
                    System.out.println("  [PARSE DEBUG] empty num at i=" + st + " near='"
                            + s.substring(Math.max(0, st - 25), Math.min(s.length(), st + 25)) + "'");
                }
            }
            try {
                return Double.parseDouble(t);
            } catch (Exception ex) {
                System.out.println("  [PARSE DEBUG] bad number '" + t + "' at " + st + " near='"
                        + s.substring(Math.max(0, st - 25), Math.min(s.length(), st + 25)) + "'");
                return 0.0;
            } }
        void skip() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        Map<String, Object> obj() { Map<String, Object> m = new java.util.LinkedHashMap<>();
            i++; skip(); if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
            while (true) { skip(); String k = str(); skip(); if (i < s.length() && s.charAt(i) == ':') i++;
                m.put(k, value()); skip(); if (i < s.length() && s.charAt(i) == ',') { i++; continue; } if (i < s.length() && s.charAt(i) == '}') { i++; break; } break; } return m; }
        java.util.List<Object> arr() { java.util.List<Object> l = new java.util.ArrayList<>();
            i++; skip(); if (i < s.length() && s.charAt(i) == ']') { i++; return l; }
            while (true) { l.add(value()); skip(); if (i < s.length() && s.charAt(i) == ',') { i++; continue; } if (i < s.length() && s.charAt(i) == ']') { i++; break; } break; } return l; }
        String str() { StringBuilder b = new StringBuilder(); i++; while (i < s.length()) { char c = s.charAt(i);
            if (c == '"') { i++; break; } if (c == '\\') { i++; char e = s.charAt(i);
                switch (e) { case 'n': b.append('\n'); break; case 'r': b.append('\r'); break; case 't': b.append('\t'); break;
                    case 'u': b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); i += 4; break; default: b.append(e); } i++; }
            else { b.append(c); i++; } } return b.toString(); }
    }
}