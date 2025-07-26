package com.salvereminder.data;

import com.google.common.collect.ImmutableSet;

import java.util.Set;
public final class SalveData {
    public static final Set<Integer> SALVE_AMULET_IDS = ImmutableSet.of(
            4081,  // Salve amulet
            10588, // Salve amulet (e)
            12017, // Salve amulet (i)
            12018, // Salve amulet (ei)
            25250, // Salve amulet (i)
            25278, // Salve amulet (ei)
            26763, // Salve amulet (i)
            26782  // Salve amulet (ei)
    );
    public static final Set<Integer> SLAYER_HELMET_IDS = ImmutableSet.of(
            11864, 11865, 19639, 19641, 19643, 19645, 19647, 19649, 21264, 21266,
            21888, 21890, 23073, 23075, 24370, 24444, 25177, 25179, 25181, 25183,
            25185, 25187, 25189, 25191, 25898, 25900, 25902, 25904, 25906, 25908,
            25910, 25912, 25914, 26674, 26675, 26676, 26677, 26678, 26679, 26680,
            26681, 26682, 26683, 26684, 29816, 29818, 29820, 29822
    );
    public static final Set<Integer> BLACK_MASK_IDS = ImmutableSet.of(
            8901, 8903, 8905, 8907, 8909, 8911, 8913, 8915, 8917, 8919, 8921,
            11774, 11775, 11776, 11777, 11778, 11779, 11780, 11781, 11782, 11783,
            11784, 25266, 25267, 25268, 25269, 25270, 25271, 25272, 25273, 25274,
            25275, 25276, 26771, 26772, 26773, 26774, 26775, 26776, 26777, 26778,
            26779, 26780, 26781
    );
    public static final Set<String> MANDATORY_SLAYER_TASKS = ImmutableSet.of(
            "aberrant spectres", "ankou", "crawling hands", "ghosts", "revenants",
            "shades", "skeletons", "vet'ion", "vorkath", "zombies"
    );
    public static final Set<String> BLUE_DRAGON_TASKS = ImmutableSet.of("blue dragons");
    public static final Set<String> OGRE_TASKS = ImmutableSet.of("ogres");
    public static final Set<Integer> UNDEAD_NPCS = ImmutableSet.of(
            2, 3, 4, 5, 6, 7,                                                                // Aberrant spectre
            7402,                                                                            // Abhorrent spectre
            2514, 2515, 2516, 2517, 2518, 2519, 6608, 7257, 7864,                            // Ankou
            12720, 12721, 12722, 12723, 12724, 12725, 12726, 12727, 12728, 12729, 12730,     // Armoured zombie
            12731, 12732, 12733, 12734, 12735, 12736, 12737, 12738, 12739, 12755, 12756,
            12757, 12758, 12759, 12760, 12761, 12762, 12763, 12764, 14113, 14114, 14115,
            14116, 14117, 14118, 14119, 14120, 14121, 14122, 14135, 14136,
            1284,                                                                            // Asyn Shade
            1283, 5632,                                                                      // Asyn Shadow
            414,                                                                             // Banshee
            12540,                                                                           // Bloodthirsty banshee
            12574,                                                                           // Bloodthirsty Repugnant spectre
            12573,                                                                           // Bloodthirsty spectre
            12542,                                                                           // Bloodthirsty twisted banshee
            11993, 11994,                                                                    // Calvar'ion
            448, 449, 450, 451, 452, 453, 454, 455, 456, 457,                                // Crawling Hand
            7388,                                                                            // Crushing hand
            7296,                                                                            // Dark Ankou
            7279,                                                                            // Deviant spectre
            1286,                                                                            // Fiyr Shade
            1285,                                                                            // Fiyr Shadow
            10523, 10524, 10525, 10526, 10534, 10535, 10536, 10537, 10544, 10545,            // Forgotten Soul
            6698, 11205,                                                                     // Ghost guard
            85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 472, 473, 474, 505,  // Ghost
            506, 507, 2527, 2528, 2529, 2530, 2531, 2532, 2533, 2534, 3625, 3975, 3976,
            3977, 3978, 3979, 5370, 7263, 7264, 12254,
            680, 681, 6440,                                                                  // Giant skeleton
            6614, 12108,                                                                     // Greater Skeleton Hellhound
            10492, 10493, 10506,                                                             // Headless Beast
            1277,                                                                            // Loar Shade
            1276,                                                                            // Loar Shadow
            5281, 5282, 5283,                                                                // Monkey Zombie
            6477,                                                                            // Mutant tarn
            8359, 10812, 10813,                                                              // Pestilent Bloat
            1280,                                                                            // Phrin Shade
            1279,                                                                            // Phrin Shadow
            7403,                                                                            // Repugnant spectre
            7934,                                                                            // Revenant cyclops
            7938,                                                                            // Revenant dark beast
            7936,                                                                            // Revenant demon
            7940,                                                                            // Revenant dragon
            7931,                                                                            // Revenant goblin
            7935,                                                                            // Revenant hellhound
            7933,                                                                            // Revenant hobgoblin
            7881,                                                                            // Revenant imp
            7939,                                                                            // Revenant knight
            11246,                                                                           // Revenant maledictus
            7937,                                                                            // Revenant ork
            7932,                                                                            // Revenant pyrefiend
            1282,                                                                            // Riyl Shade
            1281, 5631,                                                                      // Riyl Shadow
            7390,                                                                            // Screaming banshee
            7391,                                                                            // Screaming twisted banshee
            5633, 6740, 7258,                                                                // Shade
            7604, 7605, 7606,                                                                // Skeletal Mystic
            1538,                                                                            // Skeleton brute
            3358,                                                                            // Skeleton Champion
            4491, 4492, 4493, 4494, 4495, 4496, 4497, 4498, 4499,                            // Skeleton fremennik
            1540,                                                                            // Skeleton heavy
            5054, 6326, 6387, 6613, 12107,                                                   // Skeleton Hellhound
            1537,                                                                            // Skeleton hero
            84, 4319,                                                                        // Skeleton Mage
            1541,                                                                            // Skeleton thug
            1539,                                                                            // Skeleton warlord
            70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 130, 924, 1685, 1686,    // Skeleton
            1687, 1688, 2520, 2521, 2522, 2523, 2524, 2525, 2526, 3565, 3972, 3973, 3974,
            5237, 6441, 6442, 6443, 6444, 6445, 6446, 6447, 6448, 6467, 6468, 7265, 8070,
            8071, 8072, 10717, 10718, 10719, 10720, 10721, 13476, 13477, 13478, 13479,
            13495, 13496, 13497, 13498, 13499, 13500, 13501, 14426, 14427, 14428,
            872, 878, 879,                                                                   // Skogre
            882,                                                                             // Slash Bash
            69,                                                                              // Summoned Zombie
            6476,                                                                            // Tarn
            3922,                                                                            // The Draugen
            8512, 8513,                                                                      // Tormented Soul
            2999,                                                                            // Tortured soul
            8514, 8528, 8529,                                                                // Trapped Soul
            1163, 6319, 6380,                                                                // Tree spirit
            3616, 6298, 6359,                                                                // Treus Dayth
            7272,                                                                            // Twisted Banshee
            4500,                                                                            // Ulfric
            2993,                                                                            // Undead chicken
            2992,                                                                            // Undead cow
            2145,                                                                            // Undead Druid
            5648, 5649, 5650, 5651, 5652, 5653, 5654, 5655, 5656, 5657, 5658, 5659, 5660,    // Undead Lumberjack
            5661, 5662, 5663, 5665, 5666, 5667, 5668, 5669, 5670, 5671, 5672, 5673, 5674,
            5675, 5676, 5677, 5678, 5679, 5680, 5681, 5682, 5683, 5684, 5685, 5686, 5687,
            5688, 5689, 5690, 5691, 5692, 5693, 5694, 5695, 5696, 5697, 5698, 5699, 5700,
            5701, 5702, 5703, 5704, 5705, 5706, 5707, 5708, 5709, 5710, 5711, 5712, 5713,
            5714, 5715, 5716, 5717, 5718, 5719, 5720,
            5342, 5343, 5344, 5345, 5346, 5347, 5348, 5349, 5350, 5351,                      // Undead one
            10591, 10592,                                                                    // Undead Zealot
            10589,                                                                           // Urium Shade
            6611, 6612,                                                                      // Vet'ion
            8060, 8061,                                                                      // Vorkath
            866, 867, 868, 869, 870, 871, 873, 874, 875, 876, 877,                           // Zogre
            563, 564, 565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575, 576, 577,       // Zombie pirate
            578, 579, 580, 581, 582, 583, 584, 585, 586, 587, 588, 589, 590, 591, 592,
            593, 594, 595, 596, 597, 598, 599, 613, 614, 615, 616, 617, 618, 13489, 13490,
            13491, 13492, 13493,
            3969, 3970, 3971,                                                                // Zombie rat
            619, 620, 621, 622, 623, 624,                                                    // Zombie swab
            26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45,  // Zombie
            46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65,
            66, 67, 68, 880, 2501, 2502, 2503, 2504, 2505, 2506, 2507, 2508, 2509, 3980,
            3981, 5647, 6449, 6450, 6451, 6452, 6453, 6454, 6455, 6456, 6457, 6458, 6459,
            6460, 6461, 6462, 6463, 6464, 6465, 6466, 6596, 6597, 6598, 6741, 7485, 7486,
            7487, 7488, 8067, 8068, 8069,
            3359,                                                                            // Zombies Champion
            8062, 8063                                                                       // Zombified Spawn
    );
    private SalveData() {}
}