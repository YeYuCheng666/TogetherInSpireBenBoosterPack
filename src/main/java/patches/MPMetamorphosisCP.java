package patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardTarget;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.cards.colorless.Metamorphosis;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import spireTogether.SpireTogetherMod;
import spireTogether.monsters.CharacterEntity;
import spireTogether.network.P2P.P2PManager;
import spireTogether.network.P2P.P2PPlayer;
import spireTogether.network.objects.items.NetworkCard;
import spireTogether.util.Reflection;
import java.util.Iterator;

public class MPMetamorphosisCP {
    public static boolean IsMetamorphosis(AbstractCard o) {
        if (o == null) {
            return false;
        } else {
            if (o instanceof Metamorphosis){
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
    public static class MetamorphosisCP {
        public static SpireReturn Prefix(AbstractPlayer __instance) {
            if (SpireTogetherMod.isConnected && MPMetamorphosisCP.IsMetamorphosis(__instance.hoveredCard)) {
                InputHelper.justClickedLeft = false;
                __instance.hoverEnemyWaitTimer = 1.0F;
                __instance.hoveredCard.unhover();
                if (!MPMetamorphosisCP.queueContains(__instance.hoveredCard)) {
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
            clz = Metamorphosis.class,
            method = "use"
    )
    public static class MetamorphosisUseCP {
        public static SpireReturn Prefix(Metamorphosis __instance, AbstractPlayer p, AbstractMonster m) {
            if (SpireTogetherMod.isConnected && m instanceof CharacterEntity) {
                for(int i = 0; i < __instance.magicNumber; ++i) {
                    AbstractCard card = AbstractDungeon.returnTrulyRandomCardInCombat(AbstractCard.CardType.ATTACK).makeCopy();
                    if (card.cost > 0) {
                        card.cost = 0;
                        card.costForTurn = 0;
                        card.isCostModified = true;
                    }
                    ((CharacterEntity) m).addCard(NetworkCard.Generate(card), CardGroup.CardGroupType.DRAW_PILE);
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
            if (SpireTogetherMod.isConnected && MPMetamorphosisCP.IsMetamorphosis(__instance) && AbstractDungeon.player.isDraggingCard && __instance == AbstractDungeon.player.hoveredCard) {
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

