package patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardTarget;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.cards.red.Berserk;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.BerserkPower;
import com.megacrit.cardcrawl.powers.RagePower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import spireTogether.SpireTogetherMod;
import spireTogether.monsters.CharacterEntity;
import spireTogether.network.P2P.P2PManager;
import spireTogether.network.P2P.P2PPlayer;
import spireTogether.util.Reflection;
import java.util.Iterator;

public class MPBerserkCP {
    public static boolean IsBerserk(AbstractCard o) {
        if (o == null) {
            return false;
        } else {
            if (o instanceof Berserk){
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
    public static class BerserkCP {
        public static SpireReturn Prefix(AbstractPlayer __instance) {
            if (SpireTogetherMod.isConnected && MPBerserkCP.IsBerserk(__instance.hoveredCard)) {
                InputHelper.justClickedLeft = false;
                __instance.hoverEnemyWaitTimer = 1.0F;
                __instance.hoveredCard.unhover();
                if (!MPBerserkCP.queueContains(__instance.hoveredCard)) {
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
            clz = Berserk.class,
            method = "use"
    )
    public static class BerserkUseCP {
        public static SpireReturn Prefix(Berserk __instance, AbstractPlayer p, AbstractMonster m) {
            if (SpireTogetherMod.isConnected && m instanceof CharacterEntity) {
                m.addPower(new VulnerablePower(m, __instance.magicNumber, false));
                m.addPower(new BerserkPower(m, 1));
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
            if (SpireTogetherMod.isConnected && MPBerserkCP.IsBerserk(__instance) && AbstractDungeon.player.isDraggingCard && __instance == AbstractDungeon.player.hoveredCard) {
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

