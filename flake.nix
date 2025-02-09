{
  inputs = {
    nixpkgs.url = "nixpkgs";
  };

  outputs = { self, nixpkgs }: let
    system = "x86_64-linux";
    pkgs = import nixpkgs { inherit system; };

    self = with pkgs; stdenv.mkDerivation {
      pname = "cacophonyc";
      version = "0.1";
      src = ./.;

      buildInputs = [ gcc nasm jdk17 ];
      nativeBuildInputs = [ gradle_7 makeWrapper ];

      # nix build .#packages.x86_64-linux.default.mitmCache.updateScript && ./result
      mitmCache = gradle_7.fetchDeps {
        pkg = self;
        data = ./deps.json;
      };

      __darwinAllowLocalNetworking = true;

      gradleBuildTask = "shadowJar";

      preBuild = ''
        sed -i "s@lib_cacophony@$out/lib/lib_cacophony@g" src/main/kotlin/cacophony/pipeline/Params.kt
      '';

      installPhase = ''
        runHook preInstall

        mkdir $out $out/bin $out/lib
        cp -r ./lib_cacophony $out/lib/
        cp ./build/libs/tcs-1.0-SNAPSHOT-all.jar $out/lib/cacophonyc.jar

        makeWrapper ${jdk17}/bin/java $out/bin/cacophonyc \
        --add-flags "-jar $out/lib/cacophonyc.jar" \
        --set PATH ${lib.makeBinPath [ gcc nasm ]}

        runHook postInstall
      '';
    };
  in {
    packages.${system}.default = self;
  };
}
