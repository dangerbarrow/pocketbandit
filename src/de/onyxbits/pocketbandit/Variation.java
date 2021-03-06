package de.onyxbits.pocketbandit;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import java.util.Arrays;

/**
 * Constants that describe symbols, probabilities and payouts. All public fields
 * are to be considered final (they were originally, but then rule definitions moved
 * from code to JSON).
 */
public class Variation {

  /**
   * Name (and order) of the symbol textures
   */
  public String symbolNames[];
  
  /**
   * Human readable name of the machine
   */
  public String machineName;
  
  /**
   * Symbol distribution. This is a [3][x] array that gives the odds for each symbol
   * per reel. Odds are given as indexes into <code>symbols</code>.
   */
  private int[][] weightTable;
  
  /**
   * Matrix for the pay schedule. Every row is one rule. Rules with lower index have higher
   * priority. The first rule that matches is to be taken (that is: rules containing wilds
   * should be put at the end of the table). Every rule has four columns. The first three
   * columns contain indexes into <code>symbols</code> that are matched against the payline,
   * the fourth is the payout per coin bet.
   * <p>
   * The first three columns may contain the value -1 to signal "do not care" (-1 matches
   * every symbol on the reel).
   */
  public int[][] paytable;
  
  /**
   * How much money the player should be given initially for this playstyle
   */
  public int seedCapital=0;
  
  /**
   * Bonus payout when the lucky coin is played. A value of 0 or less is interpreted as
   * "game does not have this feature".
   */
  public int luckyCoinBonus = 0;
  
  /**
   * After how many rounds the lucky coin is rerolled.
   */
  public int luckyCoinReRoll = 10;
  
  /**
   * Chances for getting a bonus payout if the lucky coin is bet. This is a three member
   * array. the first member cotnains the chance if only one coin is bet, the second if 
   * two coins are bet and the third if all three coins are bet.
   */
  public float[] luckyCoinChance={0.5f, 0.25f, 0.125f};
  
  /**
   * For debugging only : not so random random picks
   */
  private int[] symbolSequence;
  
  /**
   * For debuggin: which random pick to pick next
   */
  private int symbolSequenceIndex;
  
  public Variation(){}
  
  /**
   * Match a payline against the <code>paytable</code>.
   * @param payline the three symbols on the payline (index into <code>symbolNames</code>
   * @return index into paytable or -1 if no rule matched. If several rules match, the first
   * one matching is returned (lower index=higher priority).
   */
  private int match(int[] payline) {
    for (int x=0;x<paytable.length;x++) {
      if (payline[0]==paytable[x][0] || paytable[x][0]==-1) {
        if (payline[1]==paytable[x][1] || paytable[x][1]==-1) {
          if (payline[2]==paytable[x][2] || paytable[x][2]==-1) {
            return x;
          }
        }
      }
    }
    return -1;
  }
  
  /**
   * Calculate the payout for a given payline
   * @param bet how many coins (0-3) were bet
   * @param payline the three symbols on the payline (index into <code>symbolNames</code>
   * @return -1 if the payline does not match the paytable ("player lost"). 0 if there
   * is a match, but there was no wager. Any value greater zero is how much to add to the
   * player'S casho on hand.
   */
  public int getPayout(int bet, int[] payline) {
    int tmp=match(payline);
    if (tmp==-1) {
      return -1;
    }
    else {
      return paytable[tmp][3]*bet;
    }
  }
  
  /**
   * Get the bonus payout. This method calculates the bonus payout on the assumption that
   * the lucky coin has been played.
   * @param bet how many coins were bet (0-3)
   * @return number of bonus coins to award (always 0 if no coins were bet).
   */
  public int getBonus(int bet) {
    int ret =0;
    if (bet>0 && bet <4 && SlotMachine.rng.nextFloat()<=luckyCoinChance[bet-1]) {
      ret=luckyCoinBonus;
    }
    return ret;
  }
  
  /**
   * Randomly select a new symbol.
   * @param reel which <code>weightTable</code> (0-2) to take probilities from.
   * @return the rolled image as an index into <code>symbolNames</code>
   */
  public int pick(int reel) {
    int ret = 0;
    if (symbolSequence!=null && symbolSequenceIndex<symbolSequence.length) {
      ret = symbolSequence[symbolSequenceIndex];
      symbolSequenceIndex++;
    }
    else {
      int idx = SlotMachine.rng.nextInt(weightTable[reel].length);
      ret=weightTable[reel][idx];
    }
    return ret;
  }
  
  /**
   * Query symbol faces to show on the reels initially.
   * @return 3x3 symbol faces (index into <code>symbolNames</code>) packed into a single
   * array. A new reel begins every three symbols, within the reel, symbols are ordered from
   * bottom to top.
   */
  public int[] getInitialFaces() {
    // Note: to make things easy, we just show winning combinations, so there
    // need to be at least three rules at the start of the paytable that don't
    // contain jokers.
    // Also note: This assumes that the best paying combo is the first in the
    // paytable (and should be shown on the payline).
    int[] ret = {
      paytable[1][0],
      paytable[0][0],
      paytable[2][0],
      
      paytable[1][1],
      paytable[0][1],
      paytable[2][1],
      
      paytable[1][2],
      paytable[0][2],
      paytable[2][2],
    };
    return ret;
  }
}