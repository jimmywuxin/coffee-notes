package com.coffeelab.coffeenotes.util.ocr.dictionary

/**
 * 咖啡豆标签词典集合。
 *
 * 各字段的词典独立维护，便于扩充和后续做 i18n。
 * 词条匹配时大小写不敏感，中英混排均可。
 */
object CoffeeDictionary {

    val roasterLabels: List<String> = listOf(
        "烘焙商", "烘培商", "烘培", "烘焙", "品牌", "出品", "制造", "公司",
        "Roaster", "roaster", "ROASTER", "Roasted by", "roasted by",
        "Brand", "brand", "BRAND", "Producer", "producer", "Company"
    )

    val originLabels: List<String> = listOf(
        "原料产地", "生豆产地", "原产地", "原产国", "产地", "产自", "来自",
        "国家", "Origin", "origin", "Country", "country", "From", "from", "Source", "source"
    )

    val regionLabels: List<String> = listOf(
        "产区", "区域", "Region", "region"
    )

    val varietyLabels: List<String> = listOf(
        "豆种", "品种", "Variety", "variety", "Varietal", "varietal", "Cultivar", "cultivar"
    )

    val processLabels: List<String> = listOf(
        "处理方式", "处理", "精制", "Process", "process"
    )

    val roastLevelLabels: List<String> = listOf(
        "烘焙程度", "烘焙度", "烘焙", "烘培", "Roast Level", "Roast level", "Roast", "roast"
    )

    val estateLabels: List<String> = listOf(
        "庄园", "处理站", "农庄", "农场", "合作社", "Estate", "estate", "Farm", "farm",
        "Mill", "Station", "Co-op", "Cooperative", "Washing Station", "处理厂"
    )

    /** 已知咖啡豆种。匹配时支持中英及常见变体。 */
    val knownVarieties: List<String> = listOf(
        "瑰夏", "Geisha", "gesha", "波旁", "Bourbon", "bourbon",
        "卡杜拉", "Caturra", "caturra", "卡杜艾", "Catuai", "catuai",
        "铁皮卡", "Typica", "typica", "帕卡玛拉", "Pacamara", "pacamara",
        "SL28", "SL34", "SL14", "SL24", "74158", "74112",
        "黄波旁", "红波旁", "粉波旁", "黄卡杜艾", "艺伎",
        "Wush Wush", "Pink Bourbon", "Tabi", "Sarchimor", "74110",
        "Castillo", "Colombia", "Maragogipe", "Mundonovo",
        "帕卡斯", "卡蒂姆", "Ateng", "Heirloom", "原生种"
    )

    /** 豆名识别的关键词候选。 */
    val coffeeNameKeywords: List<String> = listOf(
        "咖啡", "coffee", "豆", "bean", "Blend", "blend", "单品"
    )

    /** 常见产地（粗粒度国别）。 */
    val commonOrigins: List<String> = listOf(
        "埃塞俄比亚", "Ethiopia", "耶加雪菲", "西达摩", "古吉",
        "哥伦比亚", "Colombia", "巴西", "Brazil",
        "肯尼亚", "Kenya", "哥斯达黎加", "Costa Rica",
        "巴拿马", "Panama", "危地马拉", "Guatemala",
        "印尼", "Indonesia", "曼特宁", "苏门答腊", "Sumatra",
        "爪哇", "Java", "云南", "卢旺达", "Rwanda",
        "布隆迪", "Burundi", "秘鲁", "Peru", "洪都拉斯", "Honduras",
        "墨西哥", "Mexico", "牙买加", "Jamaica", "蓝山",
        "坦桑尼亚", "Tanzania", "厄瓜多尔", "Ecuador",
        "萨尔瓦多", "El Salvador", "尼加拉瓜", "Nicaragua",
        "巴布亚新几内亚", "PNG", "乌干达", "Uganda",
        "东帝汶", "Timor", "玻利维亚", "Bolivia",
        "赞比亚", "Zambia", "印度", "India",
        // 产地亚区
        "Sidamo", "Guji", "Yirgacheffe", "Hambela", "Sidra",
        "Antigua", "Huila", "Tarrazu", "Nariño", "Inza", "Cauca", "Brunca",
        "Kirinyaga", "Nyeri", "Murang'a", "Embu", "Meru",
        "Cajamarca"
    )

    /** 已知处理法。 */
    val knownProcesses: List<String> = listOf(
        "水洗", "日晒", "蜜处理", "厌氧", "湿刨", "washed", "natural", "honey",
        "carbonic maceration", "carbonic", "anaerobic", "anaerobic natural",
        "红酒处理", "红酒处理法", "白蜜", "黑蜜", "黄蜜", "红蜜",
        "双重厌氧", "double washed", "wet hulled", "giling basah"
    )

    /** 烘焙度词条（仅用于模糊匹配；最终仍会标准化为 浅/中/深）。 */
    val knownRoastLevels: List<String> = listOf(
        "浅度烘焙", "中度烘焙", "深度烘焙", "中深度烘焙",
        "浅烘焙", "中烘焙", "深烘焙", "浅烘", "中烘", "深烘",
        "浅中烘", "城市烘焙", "法式烘焙", "意式烘焙",
        "Light", "Medium", "City", "French", "Italian", "Full City"
    )

    /** 豆名解析时需排除的"标签前缀"。 */
    val nameSkipPrefixes: List<String> =
        roasterLabels + originLabels + regionLabels + varietyLabels +
                processLabels + roastLevelLabels + estateLabels +
                listOf("处理", "烘焙", "风味", "品种")
}
