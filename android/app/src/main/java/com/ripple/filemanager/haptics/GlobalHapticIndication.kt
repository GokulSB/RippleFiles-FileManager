package com.ripple.filemanager.haptics

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import kotlinx.coroutines.launch

class GlobalHapticIndicationNodeFactory(
    private val haptics: HapticsController,
    private val rippleFactory: IndicationNodeFactory
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        val rippleNode = rippleFactory.create(interactionSource)
        return object : DelegatingNode() {
            init {
                delegate(rippleNode)
            }
            override fun onAttach() {
                super.onAttach()
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Press) {
                            haptics.fire(com.ripple.filemanager.HapticEvent.Fab)
                        }
                    }
                }
            }
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlobalHapticIndicationNodeFactory) return false
        return haptics == other.haptics && rippleFactory == other.rippleFactory
    }
    
    override fun hashCode(): Int {
        return 31 * haptics.hashCode() + rippleFactory.hashCode()
    }
}
