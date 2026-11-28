package cz.narsil.detection.context

/**
 * Otisk Wi-Fi sítě bez polohy: brána, podsíť, DNS, DHCP server a doména z LinkProperties.
 * Android je vydá i se systémovou polohou vypnutou (na rozdíl od SSID), takže domov
 * jde poznat dál automaticky, i když si uživatel polohu v liště vypne.
 *
 * Otisk se ukládá jen ve chvíli, kdy je název sítě známý a shoduje se s domovem, proto nikdy
 * neoznačí cizí síť bez předchozího potvrzení. Riziko: jiná síť se stejným routerem a rozsahem
 * (např. 192.168.0.1/24) by bez polohy vypadala jako domov – proto otisk vyžaduje bránu i podsíť
 * a bere i DNS a DHCP server.
 */
object NetworkFingerprint {
    /**
     * @param prefixes podsítě („192.168.0.0/24“)
     * @param gateways brány výchozích tras („192.168.0.1“)
     * @return stabilní řetězec, nebo null, když síť nemá bránu nebo podsíť (otisk by byl k ničemu)
     */
    fun of(
        prefixes: Collection<String>,
        gateways: Collection<String>,
        dns: Collection<String>,
        dhcpServer: String?,
        domains: String?,
    ): String? {
        val p = prefixes.clean()
        val g = gateways.clean()
        if (p.isEmpty() || g.isEmpty()) return null
        return listOf(
            "p=" + p.joinToString(","),
            "g=" + g.joinToString(","),
            "d=" + dns.clean().joinToString(","),
            "s=" + (dhcpServer?.trim()?.removePrefix("/") ?: ""),
            "n=" + (domains?.trim()?.lowercase() ?: ""),
        ).joinToString(";")
    }

    private fun Collection<String>.clean(): List<String> =
        map { it.trim().removePrefix("/") }
            .filter { it.isNotEmpty() && it != "0.0.0.0" && it != "::" && !it.startsWith("fe80") }
            .distinct()
            .sorted()
}
