package com.schal.tv.tuner

import android.content.Context
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager

/**
 * Capteur TV matériel hors ligne.
 *
 * IMPORTANT :
 * - aucun accès Internet
 * - aucune chaîne inventée
 * - détection uniquement des tuners réellement exposés par Android
 * - le balayage RF complet dépend du matériel et du pilote constructeur
 */
class OfflineTvScanner(private val context: Context) {

    data class TunerInfo(
        val id: String,
        val name: String,
        val type: Int,
        val typeLabel: String
    )

    data class ScanResult(
        val supported: Boolean,
        val tuners: List<TunerInfo>,
        val message: String
    )

    fun detect(): ScanResult {
        val manager =
            context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
                ?: return ScanResult(
                    supported = false,
                    tuners = emptyList(),
                    message = "Le système Android ne fournit aucun gestionnaire TV."
                )

        return try {
            val tuners = manager.tvInputList
                .filter { input ->
                    !input.isPassthroughInput
                }
                .map { input ->
                    TunerInfo(
                        id = input.id,
                        name = input.loadLabel(context).toString().ifBlank {
                            "Tuner TV"
                        },
                        type = input.type,
                        typeLabel = typeName(input.type)
                    )
                }

            if (tuners.isEmpty()) {
                ScanResult(
                    supported = false,
                    tuners = emptyList(),
                    message = "Aucun tuner TV matériel accessible sur cet appareil."
                )
            } else {
                ScanResult(
                    supported = true,
                    tuners = tuners,
                    message = "${tuners.size} tuner(s) TV détecté(s)."
                )
            }

        } catch (security: SecurityException) {
            ScanResult(
                supported = false,
                tuners = emptyList(),
                message = "Accès au tuner TV refusé par Android."
            )
        } catch (error: Exception) {
            ScanResult(
                supported = false,
                tuners = emptyList(),
                message = "Erreur du capteur TV : ${error.message ?: "inconnue"}"
            )
        }
    }

    private fun typeName(type: Int): String {
        return when (type) {
            TvInputInfo.TYPE_TUNER -> "TUNER"
            TvInputInfo.TYPE_COMPOSITE -> "COMPOSITE"
            TvInputInfo.TYPE_COMPONENT -> "COMPONENT"
            TvInputInfo.TYPE_SVIDEO -> "S-VIDEO"
            TvInputInfo.TYPE_HDMI -> "HDMI"
            TvInputInfo.TYPE_DISPLAY_PORT -> "DISPLAY PORT"
            TvInputInfo.TYPE_DVI -> "DVI"
            TvInputInfo.TYPE_SCART -> "SCART"
            TvInputInfo.TYPE_VGA -> "VGA"
            else -> "TYPE $type"
        }
    }
}
