package com.coffeelab.coffeenotes.util

/**
 * 咖啡风味关键词词典。
 * 按类别分组，多词条目优先于子组件（如 "dark chocolate" 在 "chocolate" 之前）。
 */
object FlavorKeywords {
    val keywords = listOf(
        // 花香/茶香（多词优先）
        "伯爵茶", "伯爵红茶", "Earl Grey",
        "茉莉花茶", "茉莉绿茶", "Jasmine",
        "乌龙茶", "oolong",
        "红茶", "black tea",
        "绿茶", "green tea",
        "花草", "floral", "flower", "花香", "花香味",
        "玫瑰", "白玫瑰", "洛神玫瑰", "rose",
        "薰衣草", "lavender",
        "洋甘菊", "chamomile",
        "佛手柑", "bergamot",
        "茉莉花", "jasmine flower",
        "接骨木花", "elderflower",
        "香柠", "fragrant lemon", "香水柠檬",

        // 果香（多词优先）
        "热带水果", "tropical fruit", "passion fruit", "百香果",
        "深色水果", "dark fruit", "dark berry",
        "莓果类", "berries", "mixed berries", "综合莓果",
        "蓝莓", "blueberry", "黑莓", "blackberry", "覆盆子", "raspberry",
        "草莓", "strawberry",
        "甜橙", "sweet orange", "血橙", "blood orange",
        "青柚", "green grapefruit", "蜜柚", "grapefruit",
        "柑橘", "citrus", "tangerine",
        "柠檬", "lemon", "lime", "青柠",
        "橙子", "orange", "柚子", "pomelo",
        "葡萄", "grape", "葡萄汁", "葡萄柚", "wine", "葡萄酒", "winey",
        "苹果", "apple", "梨", "pear",
        "桃子", "peach", "油桃", "nectarine", "杏子", "apricot",
        "蜜瓜", "honeydew", "哈密瓜", "cantaloupe",
        "芒果", "mango", "菠萝", "pineapple", "荔枝", "lychee",
        "樱桃", "cherry", "卡西斯", "blackcurrant", "黑醋栗",
        "香蕉", "banana", "番石榴", "guava",
        "车厘子", "cherry", "树莓", "raspberry",
        "杏桃", "黄杏", "apricot",
        "水蜜桃", "juicy peach", "脆心苹果", "crisp apple", "糖心苹果", "苹果",

        // 甜感/焦糖（多词优先）
        "焦糖布丁", "caramel pudding",
        "太妃糖", "toffee", "奶油糖", "butterscotch",
        "焦糖", "caramel", "黄糖", "brown sugar", "黑糖", "muscovado",
        "枫糖", "maple syrup", "maple", "枫糖浆",
        "蜂蜜", "honey", "蜜糖", "honeyed",
        "糖蜜", "molasses",
        "奶油", "butter", "buttery", "creamy", "cream",
        "牛奶巧克力", "milk chocolate", "牛奶巧",
        "白巧克力", "white chocolate",
        "黑巧克力", "dark chocolate", "黑巧",
        "巧克力", "chocolate", "可可", "cocoa",

        // 坚果/可可（多词优先）
        "榛果巧克力", "hazelnut chocolate",
        "杏仁巧克力", "almond chocolate",
        "坚果", "坚果类", "nuts", "mixed nuts",
        "杏仁", "almond", "榛子", "hazelnut", "花生", "peanut", "walnut", "核桃",
        "开心果", "pistachio", "腰果", "cashew",
        "可可粉", "cocoa powder", "可可碎", "cocoa nibs",

        // 香料/草本（多词优先）
        "肉桂卷", "cinnamon roll",
        "香料", "spice", "spices",
        "肉桂", "cinnamon", "桂皮",
        "丁香", "clove", "多香果", "allspice",
        "香草", "vanilla", "香草荚", "vanilla bean",
        "茴香", "fennel", "八角", "star anise",
        "薄荷", "mint", "桉树", "eucalyptus",

        // 烘焙/谷物（多词优先）
        "烤吐司", "toast", "烤面包", "bread crust",
        "烤坚果", "roasted nuts", "烘烤", "roasted",
        "麦芽", "malt", "谷物", "cereal", "grain",
        "饼干", "biscuit", "曲奇", "cookie",
        "燕麦", "oat", "燕麦片",

        // 木质/烟熏（多词优先）
        "木质", "木香", "wood", "木本",
        "雪松", "cedar", "檀木", "sandalwood", "沉香", "agarwood",
        "橡木", "oak", "橡木桶", "barrel",
        "烟草", "tobacco", "烟熏", "smoky", "smoke",
        "皮革", "leather", "麝香", "musk", "muscat",

        // 发酵/酒香（多词优先）
        "威士忌", "whiskey", "whisky",
        "朗姆", "rum", "白兰地", "brandy",
        "香槟", "champagne", "气泡", "bubbly",
        "葡萄酒", "wine", "红酒", "red wine", "热红酒", "mulled wine",
        "酵母", "yeast", "发酵", "ferment",

        // 其他（多词优先）
        "芝麻", "sesame", "杏仁饼", "marzipan",
        "杏脯", "dried apricot", "葡萄干", "raisin",
        "无花果", "fig", "枣子", "date",
        "腐植土", "earthy", "土壤", "soil",
        "动物", "animal", "野味", "game",
        "麝香", "muscat", "猫尿", "cat urine" // 有些特殊处理法风味
    )
}
