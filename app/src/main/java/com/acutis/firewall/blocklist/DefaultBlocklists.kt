package com.acutis.firewall.blocklist

import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite

object DefaultBlocklists {

    fun getAdultContentDomains(): List<BlockedSite> {
        return adultDomains.map { domain ->
            BlockedSite(
                domain = domain,
                category = BlockCategory.ADULT,
                isEnabled = true,
                isCustom = false
            )
        }
    }

    fun getMalwareDomains(): List<BlockedSite> {
        return malwareDomains.map { domain ->
            BlockedSite(
                domain = domain,
                category = BlockCategory.MALWARE,
                isEnabled = true,
                isCustom = false
            )
        }
    }

    fun getGamblingDomains(): List<BlockedSite> {
        return gamblingDomains.map { domain ->
            BlockedSite(
                domain = domain,
                category = BlockCategory.GAMBLING,
                isEnabled = false,
                isCustom = false
            )
        }
    }

    fun getSocialMediaDomains(): List<BlockedSite> {
        return socialMediaDomains.map { domain ->
            BlockedSite(
                domain = domain,
                category = BlockCategory.SOCIAL_MEDIA,
                isEnabled = false,
                isCustom = false
            )
        }
    }

    fun getAllDefaultDomains(): List<BlockedSite> {
        return getAdultContentDomains() +
               getMalwareDomains() +
               getGamblingDomains() +
               getSocialMediaDomains()
    }

    private val adultDomains = listOf(
        "pornhub.com",
        "*.pornhub.com",
        "xvideos.com",
        "*.xvideos.com",
        "xnxx.com",
        "*.xnxx.com",
        "xhamster.com",
        "*.xhamster.com",
        "redtube.com",
        "*.redtube.com",
        "youporn.com",
        "*.youporn.com",
        "tube8.com",
        "*.tube8.com",
        "spankbang.com",
        "*.spankbang.com",
        "pornone.com",
        "*.pornone.com",
        "eporner.com",
        "*.eporner.com",
        "porn.com",
        "*.porn.com",
        "porntrex.com",
        "*.porntrex.com",
        "hqporner.com",
        "*.hqporner.com",
        "tnaflix.com",
        "*.tnaflix.com",
        "porndig.com",
        "*.porndig.com",
        "thumbzilla.com",
        "*.thumbzilla.com",
        "beeg.com",
        "*.beeg.com",
        "motherless.com",
        "*.motherless.com",
        "ixxx.com",
        "*.ixxx.com",
        "youjizz.com",
        "*.youjizz.com",
        "drtuber.com",
        "*.drtuber.com",
        "pornmd.com",
        "*.pornmd.com",
        "4tube.com",
        "*.4tube.com",
        "fuq.com",
        "*.fuq.com",
        "hentaihaven.xxx",
        "*.hentaihaven.xxx",
        "rule34.xxx",
        "*.rule34.xxx",
        "gelbooru.com",
        "*.gelbooru.com",
        "danbooru.donmai.us",
        "*.danbooru.donmai.us",
        "sankakucomplex.com",
        "*.sankakucomplex.com",
        "nhentai.net",
        "*.nhentai.net",
        "hitomi.la",
        "*.hitomi.la",
        "e-hentai.org",
        "*.e-hentai.org",
        "exhentai.org",
        "*.exhentai.org",
        "hanime.tv",
        "*.hanime.tv",
        "hentai2read.com",
        "*.hentai2read.com",
        "onlyfans.com",
        "*.onlyfans.com",
        "fansly.com",
        "*.fansly.com",
        "chaturbate.com",
        "*.chaturbate.com",
        "cam4.com",
        "*.cam4.com",
        "myfreecams.com",
        "*.myfreecams.com",
        "bongacams.com",
        "*.bongacams.com",
        "camsoda.com",
        "*.camsoda.com",
        "stripchat.com",
        "*.stripchat.com",
        "livejasmin.com",
        "*.livejasmin.com",
        "flirt4free.com",
        "*.flirt4free.com",
        "adultfriendfinder.com",
        "*.adultfriendfinder.com",
        "ashleymadison.com",
        "*.ashleymadison.com",
        "pornbb.org",
        "*.pornbb.org",
        "fapster.xxx",
        "*.fapster.xxx"
    )

    private val malwareDomains = listOf(
        "malware-domain.com",
        "phishing-site.net",
        "fakemicrosoft.com",
        "fakeapple.com",
        "fakegoogle.com",
        "fakeamazon.com",
        "fakepaypal.com",
        "fakebank.com",
        "tech-support-scam.com",
        "virus-alert-fake.com",
        "free-prize-winner.com",
        "click-here-to-win.com",
        "your-computer-infected.com",
        "call-microsoft-support.com",
        "urgent-security-update.com",
        "account-verify-now.com",
        "password-reset-urgent.com",
        "suspicious-activity-alert.com",
        "irs-tax-refund.com",
        "lottery-winner-claim.com",
        "prince-inheritance.com",
        "bitcoin-doubler.com",
        "crypto-giveaway-fake.com",
        "elon-musk-btc.com",
        "free-iphone-winner.com",
        "gift-card-generator.com",
        "robux-free-generator.com",
        "vbucks-generator.com",
        "survey-rewards-fake.com",
        "claim-reward-now.net"
    )

    private val gamblingDomains = listOf(
        "bet365.com",
        "*.bet365.com",
        "draftkings.com",
        "*.draftkings.com",
        "fanduel.com",
        "*.fanduel.com",
        "caesars.com",
        "*.caesars.com",
        "betmgm.com",
        "*.betmgm.com",
        "pokerstars.com",
        "*.pokerstars.com",
        "888casino.com",
        "*.888casino.com",
        "betway.com",
        "*.betway.com",
        "williamhill.com",
        "*.williamhill.com",
        "paddypower.com",
        "*.paddypower.com",
        "bovada.lv",
        "*.bovada.lv",
        "betfair.com",
        "*.betfair.com",
        "unibet.com",
        "*.unibet.com",
        "bwin.com",
        "*.bwin.com",
        "ladbrokes.com",
        "*.ladbrokes.com",
        "coral.co.uk",
        "*.coral.co.uk",
        "stake.com",
        "*.stake.com",
        "roobet.com",
        "*.roobet.com",
        "rollbit.com",
        "*.rollbit.com"
    )

    private val socialMediaDomains = listOf(
        "facebook.com",
        "*.facebook.com",
        "instagram.com",
        "*.instagram.com",
        "twitter.com",
        "*.twitter.com",
        "x.com",
        "*.x.com",
        "tiktok.com",
        "*.tiktok.com",
        "snapchat.com",
        "*.snapchat.com",
        "reddit.com",
        "*.reddit.com",
        "tumblr.com",
        "*.tumblr.com",
        "pinterest.com",
        "*.pinterest.com",
        "linkedin.com",
        "*.linkedin.com",
        "discord.com",
        "*.discord.com",
        "discordapp.com",
        "*.discordapp.com",
        "twitch.tv",
        "*.twitch.tv",
        "youtube.com",
        "*.youtube.com",
        "youtu.be",
        "*.youtu.be",
        "vimeo.com",
        "*.vimeo.com",
        "dailymotion.com",
        "*.dailymotion.com",
        "weibo.com",
        "*.weibo.com",
        "vk.com",
        "*.vk.com",
        "telegram.org",
        "*.telegram.org",
        "t.me",
        "*.t.me",
        "whatsapp.com",
        "*.whatsapp.com",
        "messenger.com",
        "*.messenger.com",
        "signal.org",
        "*.signal.org"
    )
}
