param([string]$Ffmpeg = "$PSScriptRoot/../runtime/tools/ffmpeg.exe")
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$work = Join-Path $root 'runtime/audio'
$assets = Join-Path $root 'src/main/resources/assets/neon/sounds/ui'
New-Item -ItemType Directory -Force -Path $work,$assets | Out-Null
if (!(Test-Path -LiteralPath $Ffmpeg)) { throw 'Provide a local FFmpeg executable with -Ffmpeg.' }
# Original synthesis, no sampled or third-party sound assets. Smooth envelopes avoid hard waveform edges.
Add-Type -TypeDefinition @'
using System;
using System.IO;
public static class NeonEarcons {
    const int Rate=48000;
    public static short[] Make(int kind) {
        if(kind>=6)return Chat(kind==7);
        // Neon's original soft console palette: rounded tonal beeps with a little octave warmth.
        // No noise bursts, percussive knocks, wide pitch sweeps or sampled console recordings.
        double[] durations={.090,.195,.180,.170,.220,.205};
        double[] fundamentals={659.255,587.330,493.883,659.255,739.989,554.365};
        double[] peaks={.13,.18,.165,.175,.18,.16};
        double[] bends={0,-.018,.018,0,-.035,.035};
        double[] decays={.036,.075,.065,.060,.078,.070};
        double duration=durations[kind], f=fundamentals[kind];
        short[] pcm=new short[(int)(duration*Rate)]; double[] waveform=new double[pcm.Length];
        double peak=0;
        for(int i=0;i<pcm.Length;i++) {
            double t=i/(double)Rate;
            // A tiny settling bend on actions adds motion while leaving a clear, stable note.
            double phase=2*Math.PI*f*(t+bends[kind]*.028*(1-Math.Exp(-t/.028)));
            double attack=Smooth(t/(kind==0?.009:.014));
            double release=Smooth((duration-t)/(kind==0?.040:.085));
            double envelope=attack*Math.Exp(-t/decays[kind])*release;
            // Octaves stay consonant; the upper layer fades early to avoid a piercing tail.
            double tone=Math.Sin(phase)+.12*Math.Sin(phase*.5)
                       +.045*Math.Sin(phase*2)*Math.Exp(-t/.025);
            waveform[i]=tone*envelope;
            peak=Math.Max(peak,Math.Abs(waveform[i]));
        }
        for(int i=0;i<pcm.Length;i++) pcm[i]=(short)Math.Round(waveform[i]/peak*peaks[kind]*32767);
        pcm[0]=pcm[pcm.Length-1]=0; return pcm;
    }
    static double Smooth(double value) { double x=Math.Max(0,Math.Min(1,value)); return x*x*(3-2*x); }
    static double Ping(double t,double frequency,double length) {
        if(t<0||t>=length)return 0;
        double phase=2*Math.PI*frequency*t;
        return (Math.Sin(phase)+.10*Math.Sin(phase*.5))*Smooth(t/.009)
            *Math.Exp(-t/.040)*Smooth((length-t)/.045);
    }
    static short[] Chat(bool mention) {
        double duration=mention?.230:.140,peak=0;
        double[] waveform=new double[(int)Math.Round(duration*Rate)];
        short[] pcm=new short[waveform.Length];
        for(int i=0;i<waveform.Length;i++) {
            double t=i/(double)Rate;
            waveform[i]=mention?Ping(t,659.255,.130)+.9*Ping(t-.075,987.767,.155):Ping(t,880,.140);
            peak=Math.Max(peak,Math.Abs(waveform[i]));
        }
        for(int i=0;i<pcm.Length;i++)pcm[i]=(short)Math.Round(waveform[i]/peak*.175*32767);
        pcm[0]=pcm[pcm.Length-1]=0;return pcm;
    }
    public static void Write(string file,short[] pcm) {
        using(var writer=new BinaryWriter(File.Create(file))) {
            writer.Write(System.Text.Encoding.ASCII.GetBytes("RIFF"));writer.Write(36+pcm.Length*2);
            writer.Write(System.Text.Encoding.ASCII.GetBytes("WAVEfmt "));writer.Write(16);writer.Write((short)1);writer.Write((short)1);
            writer.Write(Rate);writer.Write(Rate*2);writer.Write((short)2);writer.Write((short)16);
            writer.Write(System.Text.Encoding.ASCII.GetBytes("data"));writer.Write(pcm.Length*2);foreach(short sample in pcm)writer.Write(sample);
        }
    }
}
'@
$names=@('hover','navigate','back','confirm','enable','disable','chat_message','chat_mention')
$preview=[Collections.Generic.List[short]]::new()
for($i=0;$i -lt $names.Count;$i++) {
    $name=$names[$i]; $samples=[NeonEarcons]::Make($i)
    $wav=Join-Path $work "$name.wav"; [NeonEarcons]::Write($wav,$samples)
    Copy-Item -LiteralPath $wav -Destination (Join-Path $assets "$name.wav")
    & $Ffmpeg -hide_banner -loglevel error -y -i $wav -c:a libvorbis -q:a 6 -fflags +bitexact -flags:a +bitexact -map_metadata -1 (Join-Path $assets "$name.ogg")
    if($LASTEXITCODE -ne 0) { throw "Encoding failed for $name" }
    # Preview follows the actual default UI gain, including the quieter hover, without playing it.
    $preview.AddRange([short[]]::new(24000))
    $gain=if($i -eq 0){.4*.35}else{.4}
    $preview.AddRange([short[]]@($samples | ForEach-Object { [short][Math]::Round($_*$gain) }))
}
[NeonEarcons]::Write((Join-Path $work 'neon-ui-preview.wav'),$preview.ToArray())
Get-ChildItem -LiteralPath $assets | Select-Object Name,Length
