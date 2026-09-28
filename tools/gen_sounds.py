#!/usr/bin/env python3
"""Synthesizes all Spin Kingdom sound effects + music loop as 16-bit mono WAV files.
Everything is generated from math (sine/square/noise), no third-party samples.
Usage: python3 tools/gen_sounds.py  (writes into app/src/main/res/raw)"""
import math, os, random, struct, wave

SR = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
random.seed(42)

def note(n):  # MIDI note -> Hz
    return 440.0 * 2 ** ((n - 69) / 12.0)

def env(i, total, a=0.01, r=0.2):
    t = i / SR; dur = total / SR
    if t < a: return t / a
    if t > dur - r: return max(0.0, (dur - t) / r)
    return 1.0

def tone(freq, dur, vol=0.5, wave_="sine", a=0.005, r=0.1, slide=0.0):
    n = int(SR * dur); out = []; ph = 0.0
    for i in range(n):
        f = freq + slide * (i / n)
        ph += 2 * math.pi * f / SR
        if wave_ == "sine": s = math.sin(ph)
        elif wave_ == "square": s = 1.0 if math.sin(ph) > 0 else -1.0
        elif wave_ == "tri": s = 2 / math.pi * math.asin(math.sin(ph))
        elif wave_ == "bell": s = math.sin(ph) * 0.7 + math.sin(ph * 2.76) * 0.2 + math.sin(ph * 5.4) * 0.1
        else: s = math.sin(ph)
        out.append(s * vol * env(i, n, a, r))
    return out

def noise(dur, vol=0.5, a=0.002, r=0.2, lowpass=0.3):
    n = int(SR * dur); out = []; last = 0.0
    for i in range(n):
        last = last + lowpass * (random.uniform(-1, 1) - last)
        out.append(last * vol * env(i, n, a, r))
    return out

def mix(*tracks):
    n = max(len(t) for t in tracks); out = [0.0] * n
    for t in tracks:
        for i, s in enumerate(t): out[i] += s
    return out

def seq(*parts):
    out = []
    for p in parts: out += p
    return out

def delay(t, secs):
    return [0.0] * int(SR * secs) + t

def write(name, samples, gain=0.9):
    peak = max(0.0001, max(abs(s) for s in samples))
    k = gain / peak if peak > gain else 1.0
    path = os.path.join(OUT, name + ".wav")
    with wave.open(path, "w") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s * k)) * 32000)) for s in samples))
    print("wrote", path, len(samples) / SR, "s")

def main():
    os.makedirs(OUT, exist_ok=True)
    # click
    write("sfx_click", mix(tone(1200, 0.05, 0.6, "sine", r=0.04), noise(0.02, 0.2, r=0.015, lowpass=0.8)))
    # spin: ratchet ticks accelerating
    ticks = []
    t = 0.0
    gap = 0.07
    buf = [0.0] * int(SR * 0.9)
    while t < 0.8:
        tk = mix(tone(900 + random.uniform(-40, 40), 0.03, 0.5, "square", r=0.025), noise(0.02, 0.3, r=0.015, lowpass=0.9))
        s = int(t * SR)
        for i, v in enumerate(tk):
            if s + i < len(buf): buf[s + i] += v
        t += gap; gap = max(0.035, gap * 0.93)
    write("sfx_spin", buf)
    # reel stop
    write("sfx_reel_stop", mix(tone(220, 0.12, 0.6, "sine", slide=-80, r=0.1), noise(0.05, 0.3, r=0.04, lowpass=0.5)))
    # coin: two bell tones
    write("sfx_coin", seq(tone(note(88), 0.07, 0.5, "bell", r=0.05), tone(note(93), 0.35, 0.5, "bell", r=0.3)))
    # jackpot: fast arpeggio + shimmer
    arp = []
    for k in range(3):
        for n_ in [72, 76, 79, 84]:
            arp += tone(note(n_ + 12 * (k // 2)), 0.08, 0.4, "square", r=0.05)
    shimmer = mix(*[delay(tone(note(96 + (i % 5) * 2), 0.12, 0.2, "bell", r=0.1), 0.95 + i * 0.06) for i in range(10)])
    write("sfx_jackpot", mix(arp, shimmer, delay(tone(note(60), 1.2, 0.3, "tri", r=0.6), 0.96)))
    # spin jackpot: rising electric sweep + sparkling bell cascade + final chord
    sweep = tone(300, 0.7, 0.35, "square", slide=1500, r=0.2)
    zap = mix(*[delay(tone(note(84 + (i % 4) * 3), 0.1, 0.25, "bell", r=0.08), 0.1 + i * 0.07) for i in range(10)])
    chord = delay(mix(tone(note(72), 1.0, 0.3, "tri", r=0.7), tone(note(76), 1.0, 0.25, "tri", r=0.7), tone(note(79), 1.0, 0.25, "tri", r=0.7), tone(note(84), 1.0, 0.25, "bell", r=0.8)), 0.8)
    write("sfx_spin_jackpot", mix(sweep, zap, chord))
    # attack: cannon boom
    write("sfx_attack", mix(noise(0.8, 0.9, a=0.001, r=0.7, lowpass=0.08), tone(90, 0.6, 0.8, "sine", slide=-50, r=0.5)))
    # explosion (for building hit)
    write("sfx_explosion", mix(noise(1.0, 1.0, a=0.001, r=0.9, lowpass=0.15), tone(60, 0.8, 0.6, "sine", slide=-30, r=0.6)))
    # shield: rising shimmer + ring
    write("sfx_shield", mix(tone(400, 0.6, 0.4, "tri", slide=800, r=0.3), delay(tone(note(84), 0.6, 0.3, "bell", r=0.5), 0.25)))
    # raid dig: thud + dirt
    write("sfx_dig", mix(tone(120, 0.15, 0.8, "sine", slide=-60, r=0.12), noise(0.25, 0.5, r=0.2, lowpass=0.4)))
    # upgrade: rising sweep + chord
    write("sfx_upgrade", mix(tone(300, 0.35, 0.3, "square", slide=600, r=0.1), delay(mix(tone(note(72), 0.5, 0.3, "bell", r=0.4), tone(note(76), 0.5, 0.25, "bell", r=0.4), tone(note(79), 0.5, 0.25, "bell", r=0.4)), 0.3)))
    # level complete: fanfare
    fan = seq(tone(note(67), 0.15, 0.45, "square", r=0.05), tone(note(67), 0.15, 0.45, "square", r=0.05), tone(note(67), 0.15, 0.45, "square", r=0.05),
              tone(note(72), 0.6, 0.5, "square", r=0.3), tone(note(76), 0.2, 0.45, "square", r=0.1), tone(note(79), 0.9, 0.5, "square", r=0.6))
    bass = seq(tone(note(48), 0.45, 0.4, "tri", r=0.1), tone(note(55), 0.8, 0.4, "tri", r=0.3), tone(note(60), 1.1, 0.4, "tri", r=0.6))
    write("sfx_level_complete", mix(fan, bass))
    # chest: creak + sparkle
    creak = tone(180, 0.35, 0.4, "square", slide=120, r=0.1)
    sp = mix(*[delay(tone(note(88 + random.choice([0, 3, 5, 7, 12])), 0.15, 0.25, "bell", r=0.12), 0.3 + i * 0.05) for i in range(8)])
    write("sfx_chest", mix(creak, sp))
    # card flip
    write("sfx_card", mix(noise(0.08, 0.5, r=0.06, lowpass=0.7), tone(700, 0.08, 0.2, "sine", slide=400, r=0.06)))
    # wheel tick
    write("sfx_tick", tone(1500, 0.025, 0.5, "square", r=0.02))
    # blocked / fail
    write("sfx_fail", seq(tone(note(64), 0.18, 0.4, "square", r=0.08), tone(note(58), 0.4, 0.4, "square", r=0.3)))

    # --- background music loop: 16 bars, 110 bpm, cheerful I-V-vi-IV in C ---
    bpm = 110; beat = 60.0 / bpm
    chords = [(48, [60, 64, 67]), (43, [59, 62, 67]), (45, [60, 64, 69]), (41, [60, 65, 69])] * 4
    melody = [72, 74, 76, 79, 76, 74, 72, 67, 71, 72, 74, 71, 67, 69, 71, 74,
              76, 79, 81, 79, 76, 74, 76, 72, 74, 72, 71, 69, 72, 74, 72, 0,
              72, 76, 79, 84, 83, 79, 76, 74, 71, 74, 79, 77, 76, 74, 72, 71,
              69, 72, 76, 74, 72, 69, 72, 76, 77, 76, 74, 72, 71, 72, 74, 0]
    total = int(SR * beat * 4 * len(chords))
    buf = [0.0] * total
    def add(samples, start):
        s = int(start * SR)
        for i, v in enumerate(samples):
            j = s + i
            if j < total: buf[j] += v
    for bar, (bass_n, ch) in enumerate(chords):
        t0 = bar * 4 * beat
        for b in range(4):
            add(tone(note(bass_n), beat * 0.9, 0.22, "tri", r=0.1), t0 + b * beat)
            for c in ch:
                add(tone(note(c), beat * 0.45, 0.05, "square", r=0.08), t0 + b * beat + beat * 0.5)
        # percussion: soft hat on offbeats
        for b in range(8):
            add(noise(0.04, 0.05 if b % 2 else 0.08, r=0.03, lowpass=0.9), t0 + b * beat / 2)
    for i, m in enumerate(melody * 1):
        if m == 0: continue
        add(tone(note(m), beat * 0.95, 0.16, "bell", a=0.01, r=0.25), i * beat)
    write("music_loop", buf, gain=0.6)

if __name__ == "__main__":
    main()
