package net.goldtreeservers.worldguardextraflags.flags.handlers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;

import net.goldtreeservers.worldguardextraflags.WorldGuardExtraFlagsPlugin;
import net.goldtreeservers.worldguardextraflags.helpers.PotionEffectDetails;
import net.goldtreeservers.worldguardextraflags.utils.FlagUtils;
import net.goldtreeservers.worldguardextraflags.utils.TimeUtils;
import net.goldtreeservers.worldguardextraflags.utils.WorldGuardUtils;

public class GiveEffectsFlag extends Handler
{
	public static final Factory FACTORY = new Factory();
    public static class Factory extends Handler.Factory<GiveEffectsFlag>
    {
        @Override
        public GiveEffectsFlag create(Session session)
        {
            return new GiveEffectsFlag(session);
        }
    }

	private HashMap<PotionEffectType, PotionEffectDetails> removedEffects;
    private HashSet<PotionEffectType> givenEffects;
    
	protected GiveEffectsFlag(Session session)
	{
		super(session);
		
		this.removedEffects = new HashMap<>();
		this.givenEffects = new HashSet<>();
	}
	
	@Override
	public void initialize(Player player, Location current, ApplicableRegionSet set)
	{
		if (!WorldGuardUtils.hasBypass(player))
		{
			this.check(player, set);
		}
    }
	
	@Override
	public boolean onCrossBoundary(Player player, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType)
	{
		if (!WorldGuardUtils.hasBypass(player))
		{
			this.check(player, toSet);
		}
		
		return true;
	}
	
	@Override
	public void tick(Player player, ApplicableRegionSet set)
	{
		if (!WorldGuardUtils.hasBypass(player))
		{
			this.check(player, set);
		}
	}
	
	private void check(Player player, ApplicableRegionSet set)
	{
		Set<PotionEffect> potionEffects = set.queryValue(WorldGuardUtils.wrapPlayer(player), FlagUtils.GIVE_EFFECTS);
		if (potionEffects != null && potionEffects.size() > 0)
		{
			for (PotionEffect effect : potionEffects)
			{
				PotionEffect effect_ = null;
				for(PotionEffect activeEffect : player.getActivePotionEffects())
				{
					if (activeEffect.getType().equals(effect.getType()))
					{
						effect_ = activeEffect;
						break;
					}
				}

				if (this.givenEffects.add(effect.getType()) && effect_ != null)
				{
					if (WorldGuardExtraFlagsPlugin.isSupportsMobEffectColors())
					{
						this.removedEffects.put(effect_.getType(), new PotionEffectDetails(TimeUtils.getUnixtimestamp() + effect_.getDuration() / 20, effect_.getAmplifier(), effect_.isAmbient(), effect_.hasParticles(), effect_.getColor()));
					}
					else
					{
						this.removedEffects.put(effect_.getType(), new PotionEffectDetails(TimeUtils.getUnixtimestamp() + effect_.getDuration() / 20, effect_.getAmplifier(), effect_.isAmbient(), effect_.hasParticles(), null));
					}
					
					player.removePotionEffect(effect_.getType());
				}
				
				player.addPotionEffect(effect, true);
			}
		}
		
		Iterator<PotionEffectType> effectTypes = this.givenEffects.iterator();
		while (effectTypes.hasNext())
		{
			PotionEffectType type = effectTypes.next();
			
			if (potionEffects != null && potionEffects.size() > 0)
			{
				boolean skip = false;
				for (PotionEffect effect : potionEffects)
				{
					if (effect.getType().equals(type))
					{
						skip = true;
						break;
					}
				}
				
				if (skip)
				{
					continue;
				}
			}
			
			player.removePotionEffect(type);
			
			effectTypes.remove();
		}
		
		Iterator<Entry<PotionEffectType, PotionEffectDetails>> potionEffects_ = this.removedEffects.entrySet().iterator();
		while (potionEffects_.hasNext())
		{
			Entry<PotionEffectType, PotionEffectDetails> effect = potionEffects_.next();
			if (!this.givenEffects.contains(effect.getKey()))
			{
				PotionEffectDetails removedEffect = effect.getValue();
				if (removedEffect != null)
				{
					int timeLeft = removedEffect.getTimeLeftInTicks();
					
					if (timeLeft > 0)
					{
						if (WorldGuardExtraFlagsPlugin.isSupportsMobEffectColors())
						{
							player.addPotionEffect(new PotionEffect(effect.getKey(), timeLeft, removedEffect.getAmplifier(), removedEffect.isAmbient(), removedEffect.isParticles(), removedEffect.getColor()), true);
						}
						else
						{
							player.addPotionEffect(new PotionEffect(effect.getKey(), timeLeft, removedEffect.getAmplifier(), removedEffect.isAmbient(), removedEffect.isParticles()), true);
						}
					}
				}
				
				potionEffects_.remove();
			}
		}
	}
	
	public void drinkMilk(Player player)
	{
		this.removedEffects.clear();

		this.check(player, WorldGuardExtraFlagsPlugin.getWorldGuardPlugin().getRegionContainer().createQuery().getApplicableRegions(player.getLocation()));
	}
	
	public void drinkPotion(Player player, Collection<PotionEffect> effects)
	{
		for(PotionEffect effect : effects)
		{
			if (WorldGuardExtraFlagsPlugin.isSupportsMobEffectColors())
			{
				this.removedEffects.put(effect.getType(), new PotionEffectDetails(TimeUtils.getUnixtimestamp() + effect.getDuration() / 20, effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.getColor()));
			}
			else
			{
				this.removedEffects.put(effect.getType(), new PotionEffectDetails(TimeUtils.getUnixtimestamp() + effect.getDuration() / 20, effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), null));
			}
		}
		
		this.check(player, WorldGuardExtraFlagsPlugin.getWorldGuardPlugin().getRegionContainer().createQuery().getApplicableRegions(player.getLocation()));
	}
}
