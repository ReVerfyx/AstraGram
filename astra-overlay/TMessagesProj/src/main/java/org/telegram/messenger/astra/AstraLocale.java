package org.telegram.messenger.astra;

import org.telegram.messenger.LocaleController;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AstraLocale {
    private static final Map<String, String[]> S = new HashMap<>();

    static {
        // en, ru, uk, de, es, it, ko, nl, pt-BR, ar
        add("appearance","Appearance","Внешний вид","Вигляд","Darstellung","Apariencia","Aspetto","모양","Weergave","Aparência","المظهر");
        add("animations","Astra animations","Анимации Astra","Анімації Astra","Astra-Animationen","Animaciones de Astra","Animazioni Astra","Astra 애니메이션","Astra-animaties","Animações Astra","حركات Astra");
        add("motion_style","Motion style","Стиль анимаций","Стиль анімацій","Animationsstil","Estilo de movimiento","Stile animazioni","모션 스타일","Bewegingsstijl","Estilo de movimento","نمط الحركة");
        add("soft","Soft","Мягкий","М’який","Sanft","Suave","Morbido","부드럽게","Zacht","Suave","ناعم");
        add("normal","Normal","Обычный","Звичайний","Normal","Normal","Normale","보통","Normaal","Normal","عادي");
        add("expressive","Expressive","Выразительный","Виразний","Ausdrucksstark","Expresivo","Espressivo","강조","Expressief","Expressivo","تعبيري");
        add("automation","Automation","Автоматизация","Автоматизація","Automatisierung","Automatización","Automazione","자동화","Automatisering","Automação","الأتمتة");
        add("profile_studio","Profile Studio","Студия профиля","Студія профілю","Profilstudio","Estudio de perfil","Studio profilo","프로필 스튜디오","Profielstudio","Estúdio de perfil","استوديو الملف الشخصي");
        add("do_it_for_me","Do it for me","Сделать за меня","Зробити за мене","Für mich erledigen","Hazlo por mí","Fallo per me","대신 해줘","Doe het voor mij","Faça por mim","نفّذ عني");
        add("auto_apply","Apply approved changes automatically","Автоматически применять подтверждённые изменения","Автоматично застосовувати підтверджені зміни","Bestätigte Änderungen automatisch anwenden","Aplicar automáticamente los cambios aprobados","Applica automaticamente le modifiche approvate","승인된 변경 자동 적용","Goedgekeurde wijzigingen automatisch toepassen","Aplicar mudanças aprovadas automaticamente","تطبيق التغييرات المعتمدة تلقائيًا");
        add("ai_provider","AI provider","ИИ-провайдер","ШІ-провайдер","KI-Anbieter","Proveedor de IA","Provider IA","AI 공급자","AI-provider","Provedor de IA","مزود الذكاء الاصطناعي");
        add("provider_settings","Provider settings","Настройки ИИ","Налаштування ШІ","KI-Einstellungen","Ajustes de IA","Impostazioni IA","AI 설정","AI-instellingen","Configurações de IA","إعدادات الذكاء الاصطناعي");
        add("extensions","Extensions","Расширения","Розширення","Erweiterungen","Extensiones","Estensioni","확장 기능","Extensies","Extensões","الإضافات");
        add("plugins","Plugins","Плагины","Плагіни","Plugins","Plugins","Plugin","플러그인","Plugins","Plugins","الإضافات");
        add("profile_title","Tell AstraGram what your profile should feel like.","Опиши, каким должен быть твой профиль.","Опиши, яким має бути твій профіль.","Beschreibe, wie dein Profil wirken soll.","Describe cómo quieres que se sienta tu perfil.","Descrivi come deve apparire il tuo profilo.","프로필 분위기를 설명하세요.","Beschrijf hoe je profiel moet aanvoelen.","Descreva como seu perfil deve parecer.","صف الشكل الذي تريده لملفك الشخصي.");
        add("profile_subtitle","AstraGram will generate and apply your name, bio and avatar.","AstraGram создаст и применит имя, описание и аватар.","AstraGram створить і застосує ім’я, опис та аватар.","AstraGram erstellt und übernimmt Namen, Bio und Avatar.","AstraGram generará y aplicará tu nombre, biografía y avatar.","AstraGram genererà e applicherà nome, bio e avatar.","AstraGram이 이름, 소개 및 아바타를 생성하고 적용합니다.","AstraGram maakt en past je naam, bio en avatar toe.","AstraGram vai gerar e aplicar nome, bio e avatar.","سيُنشئ AstraGram الاسم والنبذة والصورة ويطبقها.");
        add("profile_hint","For example: dark cyber style, short bio, clean nickname...","Например: тёмный кибер-стиль, короткое описание, аккуратный ник...","Наприклад: темний кібер-стиль, короткий опис, охайний нік...","Zum Beispiel: dunkler Cyber-Stil, kurze Bio, sauberer Nickname...","Por ejemplo: estilo cyber oscuro, bio corta, apodo limpio...","Ad esempio: stile cyber scuro, bio breve, nickname pulito...","예: 어두운 사이버 스타일, 짧은 소개, 깔끔한 닉네임...","Bijvoorbeeld: donkere cyberstijl, korte bio, nette nickname...","Ex.: estilo cyber escuro, bio curta, apelido limpo...","مثال: أسلوب سايبر داكن، نبذة قصيرة، اسم أنيق...");
        add("do_it","Do it","Сделать","Зробити","Ausführen","Hacer","Fallo","실행","Uitvoeren","Fazer","تنفيذ");
        add("another_avatar","Use another avatar","Другой аватар","Інший аватар","Anderen Avatar verwenden","Usar otro avatar","Usa un altro avatar","다른 아바타 사용","Andere avatar gebruiken","Usar outro avatar","استخدام صورة أخرى");
        add("creating_profile","Creating your profile...","Создаю профиль...","Створюю профіль...","Profil wird erstellt...","Creando tu perfil...","Creazione del profilo...","프로필 생성 중...","Profiel maken...","Criando seu perfil...","جارٍ إنشاء الملف الشخصي...");
        add("applying_profile","Applying changes...","Применяю изменения...","Застосовую зміни...","Änderungen werden angewendet...","Aplicando cambios...","Applicazione modifiche...","변경 적용 중...","Wijzigingen toepassen...","Aplicando alterações...","جارٍ تطبيق التغييرات...");
        add("profile_done","Done. Profile updated.","Готово. Профиль обновлён.","Готово. Профіль оновлено.","Fertig. Profil aktualisiert.","Listo. Perfil actualizado.","Fatto. Profilo aggiornato.","완료. 프로필이 업데이트되었습니다.","Klaar. Profiel bijgewerkt.","Pronto. Perfil atualizado.","تم. تم تحديث الملف الشخصي.");
        add("ai_settings","AI settings","Настройки ИИ","Налаштування ШІ","KI-Einstellungen","Ajustes de IA","Impostazioni IA","AI 설정","AI-instellingen","Configurações de IA","إعدادات الذكاء الاصطناعي");
        add("ai_manages_channel","AI manages this channel","ИИ управляет этим каналом","ШІ керує цим каналом","KI verwaltet diesen Kanal","La IA gestiona este canal","L'IA gestisce questo canale","AI가 이 채널을 관리","AI beheert dit kanaal","IA gerencia este canal","الذكاء الاصطناعي يدير هذه القناة");
        add("channel_hint","Describe the channel. Astra AI will create 7 posts and schedule them so they publish even if your phone is offline.","Опиши канал. Astra AI создаст 7 постов и запланирует их, чтобы они вышли даже при выключенном телефоне.","Опиши канал. Astra AI створить 7 дописів і запланує їх навіть коли телефон офлайн.","Beschreibe den Kanal. Astra AI erstellt 7 Beiträge und plant sie ein.","Describe el canal. Astra AI creará 7 publicaciones y las programará.","Descrivi il canale. Astra AI creerà 7 post e li programmerà.","채널을 설명하세요. Astra AI가 게시물 7개를 만들어 예약합니다.","Beschrijf het kanaal. Astra AI maakt 7 berichten en plant ze in.","Descreva o canal. Astra AI criará 7 posts e os agendará.","صف القناة. سينشئ Astra AI سبعة منشورات ويجدولها.");
        add("channel_instruction_hint","For example: every evening short gaming news, friendly style, no clickbait...","Например: каждый вечер короткие игровые новости, дружелюбно, без кликбейта...","Наприклад: щовечора короткі ігрові новини, дружньо, без клікбейту...","Zum Beispiel: jeden Abend kurze Gaming-News, freundlich, kein Clickbait...","Por ejemplo: noticias gaming cortas cada tarde, tono amigable, sin clickbait...","Ad esempio: brevi notizie gaming ogni sera, tono amichevole, niente clickbait...","예: 매일 저녁 짧은 게임 뉴스, 친근하게, 낚시성 제목 없이...","Bijvoorbeeld: elke avond kort gamenieuws, vriendelijk, geen clickbait...","Ex.: notícias curtas de jogos toda noite, tom amigável, sem clickbait...","مثال: أخبار ألعاب قصيرة كل مساء، بأسلوب ودود ومن دون عناوين مضللة...");
        add("schedule_7","Generate and schedule 7 posts","Создать и запланировать 7 постов","Створити й запланувати 7 дописів","7 Beiträge erstellen und planen","Generar y programar 7 publicaciones","Genera e programma 7 post","게시물 7개 생성 및 예약","7 berichten maken en plannen","Gerar e agendar 7 posts","إنشاء وجدولة 7 منشورات");
        add("writing_7","Astra AI is writing seven posts...","Astra AI пишет семь постов...","Astra AI пише сім дописів...","Astra AI schreibt sieben Beiträge...","Astra AI está escribiendo siete publicaciones...","Astra AI sta scrivendo sette post...","Astra AI가 게시물 7개를 작성 중...","Astra AI schrijft zeven berichten...","Astra AI está escrevendo sete posts...","يكتب Astra AI سبعة منشورات...");
        add("scheduled_done","Done. Seven posts are scheduled.","Готово. Семь постов запланированы.","Готово. Сім дописів заплановано.","Fertig. Sieben Beiträge sind geplant.","Listo. Siete publicaciones programadas.","Fatto. Sette post programmati.","완료. 게시물 7개가 예약되었습니다.","Klaar. Zeven berichten zijn gepland.","Pronto. Sete posts agendados.","تم. تمت جدولة سبعة منشورات.");
        add("builtin_ai","Built-in Astra AI","Встроенный Astra AI","Вбудований Astra AI","Integrierte Astra AI","Astra AI integrada","Astra AI integrata","내장 Astra AI","Ingebouwde Astra AI","Astra AI integrada","Astra AI المدمج");
        add("custom_endpoint","Custom HTTPS endpoint (optional)","Свой HTTPS endpoint (необязательно)","Власний HTTPS endpoint (необов’язково)","Eigener HTTPS-Endpunkt (optional)","Endpoint HTTPS propio (opcional)","Endpoint HTTPS personalizzato (opzionale)","사용자 HTTPS 엔드포인트(선택)","Eigen HTTPS-endpoint (optioneel)","Endpoint HTTPS próprio (opcional)","نقطة HTTPS مخصصة (اختياري)");
        add("model","Model","Модель","Модель","Modell","Modelo","Modello","모델","Model","Modelo","النموذج");
        add("api_key_optional","API key (optional)","API-ключ (необязательно)","API-ключ (необов’язково)","API-Schlüssel (optional)","Clave API (opcional)","Chiave API (opzionale)","API 키(선택)","API-sleutel (optioneel)","Chave API (opcional)","مفتاح API (اختياري)");
        add("provider_note","Leave endpoint and model empty to use built-in Astra AI. The server address is hidden from this screen.","Оставь endpoint и модель пустыми, чтобы использовать встроенный Astra AI. Адрес сервера на этом экране скрыт.","Залиш endpoint і модель порожніми для вбудованого Astra AI. Адресу сервера приховано.","Endpoint und Modell leer lassen, um die integrierte Astra AI zu nutzen. Die Serveradresse ist ausgeblendet.","Deja endpoint y modelo vacíos para usar Astra AI integrada. La dirección del servidor está oculta.","Lascia endpoint e modello vuoti per usare Astra AI integrata. L'indirizzo del server è nascosto.","내장 Astra AI를 사용하려면 엔드포인트와 모델을 비워 두세요. 서버 주소는 숨겨집니다.","Laat endpoint en model leeg voor de ingebouwde Astra AI. Het serveradres is verborgen.","Deixe endpoint e modelo vazios para usar Astra AI integrada. O endereço do servidor fica oculto.","اترك نقطة الاتصال والنموذج فارغين لاستخدام Astra AI المدمج. عنوان الخادم مخفي.");
        add("provider_saved","Provider saved","Провайдер сохранён","Провайдер збережено","Anbieter gespeichert","Proveedor guardado","Provider salvato","공급자 저장됨","Provider opgeslagen","Provedor salvo","تم حفظ المزود");
        add("https_only","Use an HTTPS endpoint","Используй HTTPS endpoint","Використовуй HTTPS endpoint","HTTPS-Endpunkt verwenden","Usa un endpoint HTTPS","Usa un endpoint HTTPS","HTTPS 엔드포인트를 사용하세요","Gebruik een HTTPS-endpoint","Use um endpoint HTTPS","استخدم نقطة HTTPS");
        add("set_both","Set both endpoint and model","Укажи и endpoint, и модель","Вкажи endpoint і модель","Endpoint und Modell angeben","Indica endpoint y modelo","Imposta endpoint e modello","엔드포인트와 모델을 모두 입력하세요","Vul endpoint en model in","Informe endpoint e modelo","حدد نقطة الاتصال والنموذج");
        add("proxy_auto","Automatic MTProto proxy search","Автопоиск MTProto-прокси","Автопошук MTProto-проксі","Automatische MTProto-Proxy-Suche","Búsqueda automática de proxy MTProto","Ricerca automatica proxy MTProto","MTProto 프록시 자동 검색","Automatisch MTProto-proxy zoeken","Busca automática de proxy MTProto","بحث تلقائي عن وكيل MTProto");
        add("proxy_auto_info","If the current MTProto proxy fails, AstraGram finds fresh public proxies, checks them and switches to a working one.","Если текущий MTProto-прокси не работает, AstraGram найдёт свежие публичные прокси, проверит их и переключится на рабочий.","Якщо поточний MTProto-проксі не працює, AstraGram знайде нові публічні проксі, перевірить їх і перемкнеться.","Wenn der aktuelle MTProto-Proxy ausfällt, sucht AstraGram neue öffentliche Proxys und wechselt automatisch.","Si falla el proxy MTProto actual, AstraGram busca proxies públicos nuevos, los comprueba y cambia automáticamente.","Se il proxy MTProto corrente non funziona, AstraGram trova proxy pubblici nuovi, li verifica e passa a uno funzionante.","현재 MTProto 프록시가 실패하면 AstraGram이 새 공개 프록시를 찾아 확인 후 전환합니다.","Als de huidige MTProto-proxy niet werkt, zoekt AstraGram nieuwe openbare proxies en schakelt automatisch.","Se o proxy MTProto atual falhar, AstraGram encontra proxies públicos novos, testa e troca automaticamente.","إذا تعطل وكيل MTProto الحالي، يبحث AstraGram عن وكلاء عامين جدد ويفحصهم ثم ينتقل إلى وكيل يعمل.");
    }

    private AstraLocale() {}

    private static void add(String key, String... values) {
        S.put(key, values);
    }

    public static String get(String key) {
        String[] v = S.get(key);
        if (v == null || v.length == 0) return key;
        int i = index();
        return i >= 0 && i < v.length ? v[i] : v[0];
    }

    private static int index() {
        String lang = "en";
        try {
            LocaleController.LocaleInfo info = LocaleController.getInstance().getCurrentLocaleInfo();
            if (info != null && info.getBaseLangCode() != null) {
                lang = info.getBaseLangCode().toLowerCase(Locale.US);
            }
        } catch (Throwable ignored) {}
        if (lang.startsWith("ru")) return 1;
        if (lang.startsWith("uk")) return 2;
        if (lang.startsWith("de")) return 3;
        if (lang.startsWith("es")) return 4;
        if (lang.startsWith("it")) return 5;
        if (lang.startsWith("ko")) return 6;
        if (lang.startsWith("nl")) return 7;
        if (lang.startsWith("pt")) return 8;
        if (lang.startsWith("ar")) return 9;
        return 0;
    }
}
