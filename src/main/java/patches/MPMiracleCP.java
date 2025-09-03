package patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardTarget;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.cards.tempCards.Miracle;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import spireTogether.SpireTogetherMod;
import spireTogether.monsters.CharacterEntity;
import spireTogether.network.P2P.P2PManager;
import spireTogether.network.P2P.P2PPlayer;
import spireTogether.util.Reflection;
import java.util.Iterator;

public class MPMiracleCP {
    public static boolean IsMiracle(AbstractCard o) {
        if (o == null) {
            return false;
        } else {
            if (o instanceof Miracle){
                return true;
            }
            return false;
        }
    }

    private static boolean queueContains(AbstractCard card) {
        for(CardQueueItem i : AbstractDungeon.actionManager.cardQueue) {
            if (i.card == card) {
                return true;
            }
        }
        return false;
    }

    @SpirePatch2(
            clz = AbstractPlayer.class,
            method = "playCard"
    )
    public static class MiracleCP {
        public static SpireReturn Prefix(AbstractPlayer __instance) {
            if (SpireTogetherMod.isConnected && MPMiracleCP.IsMiracle(__instance.hoveredCard)) {
                InputHelper.justClickedLeft = false;
                __instance.hoverEnemyWaitTimer = 1.0F;
                __instance.hoveredCard.unhover();
                if (!MPMiracleCP.queueContains(__instance.hoveredCard)) {
                    AbstractMonster hoveredMonster = (AbstractMonster)Reflection.getFieldValue("hoveredMonster", __instance);
                    if (hoveredMonster instanceof CharacterEntity) {
                        AbstractDungeon.actionManager.cardQueue.add(new CardQueueItem(__instance.hoveredCard, hoveredMonster));
                    } else {
                        AbstractDungeon.actionManager.cardQueue.add(new CardQueueItem(__instance.hoveredCard, (AbstractMonster)null));
                    }
                }

                Reflection.setFieldValue("isUsingClickDragControl", __instance, false);
                __instance.hoveredCard = null;
                __instance.isDraggingCard = false;
                return SpireReturn.Return();
            } else {
                return SpireReturn.Continue();
            }
        }
    }

    @SpirePatch(
            clz = Miracle.class,
            method = "use"
    )
    public static class MiracleUseCP {
        public static SpireReturn Prefix(Miracle __instance, AbstractPlayer p, AbstractMonster m) {
            if (SpireTogetherMod.isConnected && m instanceof CharacterEntity) {
                if (__instance.upgraded) {
                    ((CharacterEntity) m).gainEnergy(2);
                } else {
                    ((CharacterEntity) m).gainEnergy(1);
                }
                return SpireReturn.Return();
            } else {
                return SpireReturn.Continue();
            }
        }
    }

    @SpirePatch(
            clz = AbstractCard.class,
            method = "update"
    )
    public static class TargetPlayersClass {
        public static void Prefix(AbstractCard __instance) {
            if (SpireTogetherMod.isConnected && MPMiracleCP.IsMiracle(__instance) && AbstractDungeon.player.isDraggingCard && __instance == AbstractDungeon.player.hoveredCard) {
                boolean shouldNotReset = false;
                Iterator pI = P2PManager.GetAllPlayers(false);

                while(pI.hasNext()) {
                    P2PPlayer next = (P2PPlayer)pI.next();
                    if (next.IsPlayerInSameRoomAndAction() && next.IsTechnicallyAlive()) {
                        CharacterEntity entity = next.GetEntity();
                        if (entity != null && (entity.hb.hovered || next.GetInfobox().IsHovered())) {
                            __instance.target = CardTarget.ENEMY;
                            shouldNotReset = true;
                        }
                    }
                }

                if (!shouldNotReset) {
                    if (AbstractDungeon.player.inSingleTargetMode) {
                        AbstractDungeon.player.inSingleTargetMode = false;
                    }

                    __instance.target = CardTarget.SELF;
                }
            }

        }
    }
}

