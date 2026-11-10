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
        double[] durations={.055,.075,.065,.085};
        double[] frequencies={720,1040,640,880};
        double duration=durations[kind], f=frequencies[kind];
        short[] pcm=new short[(int)(duration*Rate)]; Random random=new Random(711+kind); double noise=0;
        for(int i=0;i<pcm.Length;i++) {
            double t=i/(double)Rate, sweep=kind==1?260:kind==2?-150:-100;
            double phase=2*Math.PI*(f*t+sweep*t*t/(2*duration));
            noise=.72*noise+.28*(random.NextDouble()*2-1);
            double sample=.30*Math.Sin(phase)*Math.Exp(-t/0.012)
                +.20*Math.Sin(2*Math.PI*180*t)*Math.Exp(-t/0.008)
                +.12*noise*Math.Exp(-t/0.003);
            if(kind==1||kind==3) {
                double delayed=t-.011;
                if(delayed>0) sample+=.10*Math.Sin(2*Math.PI*f*1.5*delayed)*(1-Math.Exp(-delayed/.001))*Math.Exp(-delayed/.013);
            }
            double attack=1-Math.Exp(-t/.0007);
            double tail=Math.Min(1,(duration-t)/.009); tail=tail*tail*(3-2*tail);
            pcm[i]=(short)Math.Round(Math.Clamp(sample*attack*tail,-.8,.8)*32767);
        }
        pcm[0]=pcm[pcm.Length-1]=0; return pcm;
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
$names=@('enable')
$preview=[Collections.Generic.List[short]]::new()
for($i=0;$i -lt $names.Count;$i++) {
    $name=$names[$i]; $samples=[NeonEarcons]::Make(1)
    $wav=Join-Path $work "$name.wav"; [NeonEarcons]::Write($wav,$samples)
    Copy-Item -LiteralPath $wav -Destination (Join-Path $assets "$name.wav")
    & $Ffmpeg -hide_banner -loglevel error -y -i $wav -c:a libvorbis -q:a 6 -map_metadata -1 (Join-Path $assets "$name.ogg")
    if($LASTEXITCODE -ne 0) { throw "Encoding failed for $name" }
    $preview.AddRange([short[]]::new(12000)); $preview.AddRange($samples)
}
[NeonEarcons]::Write((Join-Path $work 'neon-ui-preview.wav'),$preview.ToArray())
Get-ChildItem -LiteralPath $assets | Select-Object Name,Length
