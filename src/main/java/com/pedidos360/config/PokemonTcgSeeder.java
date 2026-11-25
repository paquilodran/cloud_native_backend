package com.pedidos360.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.entity.Carta;
import com.pedidos360.repository.CartaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga el catalogo de cartas al iniciar: primero desde la API publica pokemontcg.io
 * (set Base) y, si no hay salida a internet, desde un catalogo semilla embebido.
 */
@Component
public class PokemonTcgSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PokemonTcgSeeder.class);
    private static final String API_URL =
            "https://api.pokemontcg.io/v2/cards?q=set.id:base1&pageSize=102&select=id,name,types,rarity,hp,images";

    private final CartaRepository cartaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.tcg.api-enabled:true}")
    private boolean apiEnabled;

    public PokemonTcgSeeder(CartaRepository cartaRepository) {
        this.cartaRepository = cartaRepository;
    }

    @Override
    public void run(String... args) {
        if (cartaRepository.count() > 0) {
            return;
        }
        List<Carta> cartas = apiEnabled ? fetchFromApi() : new ArrayList<>();
        if (cartas.isEmpty()) {
            log.warn("[TCG-SEED] API pokemontcg.io no disponible: usando catalogo semilla embebido");
            cartas = seedCatalog();
        } else {
            log.info("[TCG-SEED] {} cartas importadas desde pokemontcg.io", cartas.size());
        }
        cartaRepository.saveAll(cartas);
    }

    private List<Carta> fetchFromApi() {
        List<Carta> result = new ArrayList<>();
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return result;
            }
            JsonNode data = objectMapper.readTree(response.body()).path("data");
            for (JsonNode node : data) {
                String rareza = node.path("rarity").asText("Common");
                String tipo = node.path("types").isArray() && node.path("types").size() > 0
                        ? node.path("types").get(0).asText("Colorless") : "Colorless";
                Integer hp = node.path("hp").asText("").matches("\\d+") ? Integer.valueOf(node.path("hp").asText()) : null;
                result.add(new Carta(
                        node.path("id").asText(),
                        node.path("name").asText(),
                        tipo,
                        rareza,
                        hp,
                        node.path("images").path("small").asText(null),
                        precioPorRareza(rareza)));
            }
        } catch (Exception e) {
            log.warn("[TCG-SEED] No se pudo consultar pokemontcg.io: {}", e.getMessage());
            result.clear();
        }
        return result;
    }

    static BigDecimal precioPorRareza(String rareza) {
        if (rareza == null) {
            return BigDecimal.valueOf(1500);
        }
        String r = rareza.toLowerCase();
        if (r.contains("holo")) {
            return BigDecimal.valueOf(25000);
        }
        if (r.contains("rare")) {
            return BigDecimal.valueOf(8000);
        }
        if (r.contains("uncommon")) {
            return BigDecimal.valueOf(3000);
        }
        return BigDecimal.valueOf(1500);
    }

    private static Carta c(String id, String nombre, String tipo, String rareza, int hp) {
        String numero = id.substring(id.indexOf('-') + 1);
        return new Carta(id, nombre, tipo, rareza, hp,
                "https://images.pokemontcg.io/base1/" + numero + ".png", precioPorRareza(rareza));
    }

    private List<Carta> seedCatalog() {
        List<Carta> l = new ArrayList<>();
        l.add(c("base1-1", "Alakazam", "Psychic", "Rare Holo", 80));
        l.add(c("base1-2", "Blastoise", "Water", "Rare Holo", 100));
        l.add(c("base1-3", "Chansey", "Colorless", "Rare Holo", 120));
        l.add(c("base1-4", "Charizard", "Fire", "Rare Holo", 120));
        l.add(c("base1-5", "Clefairy", "Colorless", "Rare Holo", 40));
        l.add(c("base1-6", "Gyarados", "Water", "Rare Holo", 100));
        l.add(c("base1-7", "Hitmonchan", "Fighting", "Rare Holo", 70));
        l.add(c("base1-8", "Machamp", "Fighting", "Rare Holo", 100));
        l.add(c("base1-9", "Magneton", "Lightning", "Rare Holo", 60));
        l.add(c("base1-10", "Mewtwo", "Psychic", "Rare Holo", 60));
        l.add(c("base1-11", "Nidoking", "Grass", "Rare Holo", 90));
        l.add(c("base1-12", "Ninetales", "Fire", "Rare Holo", 80));
        l.add(c("base1-13", "Poliwrath", "Water", "Rare Holo", 90));
        l.add(c("base1-14", "Raichu", "Lightning", "Rare Holo", 80));
        l.add(c("base1-15", "Venusaur", "Grass", "Rare Holo", 100));
        l.add(c("base1-16", "Zapdos", "Lightning", "Rare Holo", 90));
        l.add(c("base1-44", "Bulbasaur", "Grass", "Common", 40));
        l.add(c("base1-46", "Charmander", "Fire", "Common", 50));
        l.add(c("base1-58", "Pikachu", "Lightning", "Common", 40));
        l.add(c("base1-63", "Squirtle", "Water", "Common", 40));
        return l;
    }
}
