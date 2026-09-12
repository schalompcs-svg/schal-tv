package com.schal.tv.core

/**
 * Touches de navigation abstraites, indépendantes de l'interface tactile
 * Android. Une future UI "SCHAL BASIC" pour téléphone à touches (clavier
 * physique) pourra réutiliser exactement cette même logique de navigation
 * sans toucher au code métier.
 */
enum class NavKey { UP, DOWN, LEFT, RIGHT, OK, BACK, MENU }

/**
 * Contrat que doit implémenter tout écran capable d'être piloté au clavier
 * physique (D-pad, touches numériques) en plus du tactile. MainActivity et
 * PlayerActivity implémentent cette interface dès la v0.2 pour que le futur
 * portage n'exige pas de réécrire la logique de sélection/lecture.
 */
interface NavigationController {
    /** Retourne true si la touche a été consommée par l'écran. */
    fun onNavKey(key: NavKey): Boolean
}
